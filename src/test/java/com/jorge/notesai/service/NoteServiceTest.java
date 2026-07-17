package com.jorge.notesai.service;

import com.jorge.notesai.dto.NoteDtos.NoteRequest;
import com.jorge.notesai.dto.NoteDtos.NoteResponse;
import com.jorge.notesai.entity.Note;
import com.jorge.notesai.entity.User;
import com.jorge.notesai.repository.NoteRepository;
import com.jorge.notesai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private NoteService noteService;

    private User owner;
    private Note note;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("jorge@example.com").name("Jorge").build();
        note = Note.builder().id(10L).title("Shopping").content("Milk and eggs").owner(owner).build();
    }

    @Test
    void getAllForUser_shouldReturnOnlyTheUsersNotes() {
        when(userRepository.findByEmail("jorge@example.com")).thenReturn(Optional.of(owner));
        when(noteRepository.findByOwnerOrderByCreatedAtDesc(owner)).thenReturn(List.of(note));

        List<NoteResponse> result = noteService.getAllForUser("jorge@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Shopping");
    }

    @Test
    void create_shouldSaveNoteAssociatedWithTheUser() {
        NoteRequest request = new NoteRequest("New note", "Test content");

        when(userRepository.findByEmail("jorge@example.com")).thenReturn(Optional.of(owner));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        NoteResponse response = noteService.create("jorge@example.com", request);

        assertThat(response).isNotNull();
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void update_shouldThrowException_whenNoteDoesNotBelongToUser() {
        NoteRequest request = new NoteRequest("Edited title", "Edited content");

        when(userRepository.findByEmail("jorge@example.com")).thenReturn(Optional.of(owner));
        when(noteRepository.findByIdAndOwner(99L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.update("jorge@example.com", 99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to you");

        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void delete_shouldRemoveNote_whenItBelongsToUser() {
        when(userRepository.findByEmail("jorge@example.com")).thenReturn(Optional.of(owner));
        when(noteRepository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(note));

        noteService.delete("jorge@example.com", 10L);

        verify(noteRepository).delete(note);
    }

    @Test
    void summarize_shouldSaveAiSummaryOnTheNote() {
        when(userRepository.findByEmail("jorge@example.com")).thenReturn(Optional.of(owner));
        when(noteRepository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(note));
        when(geminiService.summarize(note.getContent())).thenReturn("Resumen de prueba");
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        noteService.summarize("jorge@example.com", 10L);

        verify(geminiService).summarize(note.getContent());
        verify(noteRepository).save(argThat(n -> "Resumen de prueba".equals(n.getAiSummary())));
    }
}