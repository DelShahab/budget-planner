package com.budgetplanner.budget.service;

import com.budgetplanner.budget.model.BankTransaction;
import com.budgetplanner.budget.model.TransactionNote;
import com.budgetplanner.budget.model.TransactionTag;
import com.budgetplanner.budget.repository.TransactionNoteRepository;
import com.budgetplanner.budget.repository.TransactionTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionMetaService {

    private final TransactionNoteRepository noteRepository;
    private final TransactionTagRepository tagRepository;

    public TransactionMetaService(TransactionNoteRepository noteRepository,
                                  TransactionTagRepository tagRepository) {
        this.noteRepository = noteRepository;
        this.tagRepository = tagRepository;
    }

    public String getNoteForTransaction(BankTransaction transaction) {
        return noteRepository.findByBankTransaction(transaction)
                .map(TransactionNote::getNoteText)
                .orElse("");
    }

    public List<String> getTagsForTransaction(BankTransaction transaction) {
        return tagRepository.findByBankTransaction(transaction).stream()
                .map(TransactionTag::getTag)
                .collect(Collectors.toList());
    }

    /**
     * Get all distinct tag names used across any transaction.
     */
    public List<String> getAllTagNames() {
        return tagRepository.findDistinctTagNames();
    }

    public void saveNoteAndTags(BankTransaction transaction, String noteText, String tagsCsv) {
        // Note
        if (noteText == null || noteText.trim().isEmpty()) {
            noteRepository.deleteByBankTransaction(transaction);
        } else {
            TransactionNote note = noteRepository.findByBankTransaction(transaction)
                    .orElseGet(TransactionNote::new);
            note.setBankTransaction(transaction);
            note.setNoteText(noteText.trim());
            noteRepository.save(note);
        }

        // Tags
        tagRepository.deleteByBankTransaction(transaction);
        if (tagsCsv != null && !tagsCsv.trim().isEmpty()) {
            List<String> tags = Arrays.stream(tagsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            for (String tag : tags) {
                TransactionTag txTag = new TransactionTag();
                txTag.setBankTransaction(transaction);
                txTag.setTag(tag);
                tagRepository.save(txTag);
            }
        }
    }
}
