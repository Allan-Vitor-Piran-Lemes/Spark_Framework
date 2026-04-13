package org.example.service;

import org.example.exception.UserNotFoundException;
import org.example.model.User;
import org.example.repository.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository repository = new UserRepository();

    public void create(User user) {
        if (repository.findByCpf(user.getCpf()).isPresent()) {
            throw new RuntimeException("Erro: Este CPF já está cadastrado no sistema.");
        }
        repository.save(user);
    }

    public List<User> getAll() {
        return repository.findAll();
    }

    public User getByCpf(String cpf) {
        try {
            User user = repository.findByCpf(cpf).orElse(null);

            if (user == null) {
                throw new UserNotFoundException("Usuário não encontrado no sistema.");
            }
            return user;
        } catch (UserNotFoundException e) {
            throw new RuntimeException("Erro: " + e.getMessage());
        }
    }

    public void update(String cpf, User user) {
        getByCpf(cpf);
        user.setCpf(cpf);
        repository.update(user);
    }

    public void remove(String cpf) {
        getByCpf(cpf);
        repository.delete(cpf);
    }
}
