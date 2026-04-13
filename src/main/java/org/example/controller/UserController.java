package org.example.controller;

import org.example.exception.ErrorResponse;
import org.example.service.UserService;
import com.google.gson.Gson;
import org.example.model.User;
import static spark.Spark.*;

public class UserController {
    private final UserService service = new UserService();
    private final Gson gson = new Gson();

    public void routes() {
        // Configuração de CORS para o Scalar funcionar no navegador
        // O método options captura a requisição de 'preflight' do navegador
        options("/*", (req, res) -> {
            String headers = req.headers("Access-Control-Request-Headers");
            if (headers != null) {
                res.header("Access-Control-Allow-Headers", headers);
            }

            // Esta linha avisa ao navegador que o servidor aceita PUT e DELETE
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

            return "OK";
        });

        // Garante que todos os cabeçalhos de permissão sejam enviados em cada resposta
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        });

        // Listar todos (GET)
        get("/users", (req, res) -> service.getAll(), gson::toJson);

        // Buscar um (GET)
        get("/user/:cpf", (req, res) -> service.getByCpf(req.params(":cpf")), gson::toJson);

        // Criar (POST)
        post("/user", (req, res) -> {
            User user = gson.fromJson(req.body(), User.class);
            service.create(user);
            res.status(201);
            return "Usuário cadastrado com sucesso!";
        });

        // Atualizar (PUT)
        put("/user/:cpf", (req, res) -> {
            User user = gson.fromJson(req.body(), User.class);
            service.update(req.params(":cpf"), user);
            res.status(200);
            res.type("application/json");
            return gson.toJson(new ErrorResponse("Dados atualizados com sucesso!"));
        });

        // Remover (DELETE)
        delete("/user/:cpf", (req, res) -> {
            service.remove(req.params(":cpf"));
            res.status(200);
            res.type("application/json");
            return gson.toJson(new ErrorResponse("Usuário removido com sucesso!"));
        });
    }
}