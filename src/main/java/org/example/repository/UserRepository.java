package org.example.repository;

import org.example.model.User;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class UserRepository {
    private static final List<User> users = new ArrayList<>();

    public void save(User user) {
        users.add(user);
    }

    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    public Optional<User> findByCpf(String cpf) {
        return users.stream().filter(u -> u.getCpf().equals(cpf)).findFirst();
    }

    public void update(User user) {
        Optional<User> existingUserOpt = findByCpf(user.getCpf());

        if (existingUserOpt.isPresent()) {
            users.remove(existingUserOpt.get());
            users.add(user);
        } else {
            throw new RuntimeException("Usuário com CPF " + user.getCpf() + " não encontrado para atualização");
        }
    }

    public void delete(String cpf) {
        users.removeIf(u -> u.getCpf().equals(cpf));
    }
}
