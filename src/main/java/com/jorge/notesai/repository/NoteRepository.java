package com.jorge.notesai.repository;

import com.jorge.notesai.entity.Note;
import com.jorge.notesai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByOwnerOrderByCreatedAtDesc(User owner);
    Optional<Note> findByIdAndOwner(Long id, User owner);
}
