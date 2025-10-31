package com.example.project.student.service;

import com.example.project.student.model.Student;
import com.example.project.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentService {
    private final StudentRepository repo;

    public StudentService(StudentRepository repo) {
        this.repo = repo;
    }

    public Student create(Student s) {
        if (s.getId() == null) s.setId(UUID.randomUUID());
        return repo.save(s);
    }

    @Transactional(readOnly = true)
    public Optional<Student> findById(UUID id) {
        return repo.findById(id);
    }

    public List<Student> findAll() {
        return repo.findAll();
    }

    public void delete(UUID id) {
        repo.deleteById(id);
    }

    public Student update(Student s) {
        return repo.save(s);
    }

    public boolean chargeCoins(UUID studentId, BigDecimal amount) {
        Student st = repo.findById(studentId).orElseThrow();
        BigDecimal balance = st.getCoins() == null ? BigDecimal.ZERO : st.getCoins();
        if (balance.compareTo(amount) < 0) return false;
        st.setCoins(balance.subtract(amount));
        repo.save(st);
        return true;
    }
}
