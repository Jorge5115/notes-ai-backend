package com.jorge.notesai.service;

import com.jorge.notesai.dto.NoteDtos.NoteRequest;
import com.jorge.notesai.dto.NoteDtos.NoteResponse;
import com.jorge.notesai.entity.Note;
import com.jorge.notesai.entity.User;
import com.jorge.notesai.repository.NoteRepository;
import com.jorge.notesai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    public List<NoteResponse> getAllForUser(String email) {
        User owner = getUser(email);
        return noteRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NoteResponse create(String email, NoteRequest request) {
        User owner = getUser(email);

        Note note = Note.builder()
                .title(request.title())
                .content(request.content())
                .owner(owner)
                .build();

        return toResponse(noteRepository.save(note));
    }

    public NoteResponse update(String email, Long noteId, NoteRequest request) {
        User owner = getUser(email);
        Note note = getOwnedNote(noteId, owner);

        note.setTitle(request.title());
        note.setContent(request.content());
        note.setUpdatedAt(LocalDateTime.now());

        return toResponse(noteRepository.save(note));
    }

    public void delete(String email, Long noteId) {
        User owner = getUser(email);
        Note note = getOwnedNote(noteId, owner);
        noteRepository.delete(note);
    }

    public NoteResponse summarize(String email, Long noteId) {
        User owner = getUser(email);
        Note note = getOwnedNote(noteId, owner);

        String summary = geminiService.summarize(note.getContent());
        note.setAiSummary(summary);

        return toResponse(noteRepository.save(note));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Note getOwnedNote(Long noteId, User owner) {
        return noteRepository.findByIdAndOwner(noteId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Note not found or does not belong to you"));
    }

    private NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getAiSummary(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}