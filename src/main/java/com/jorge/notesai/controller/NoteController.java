package com.jorge.notesai.controller;

import com.jorge.notesai.dto.NoteDtos.NoteRequest;
import com.jorge.notesai.dto.NoteDtos.NoteResponse;
import com.jorge.notesai.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(noteService.getAllForUser(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(Authentication auth, @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(noteService.create(auth.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> update(Authentication auth, @PathVariable Long id,
                                                @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(noteService.update(auth.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long id) {
        noteService.delete(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
