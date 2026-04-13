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
        options("/*", (req, res) -> {
            String headers = req.headers("Access-Control-Request-Headers");
            if (headers != null) {
                res.header("Access-Control-Allow-Headers", headers);
            }
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            return "OK";
        });

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.type("application/json");
        });

        get("/users", (req, res)
                -> service.getAll(), gson::toJson);

        get("/user/:cpf", (req, res) -> service.getByCpf(req.params(":cpf")), gson::toJson);

        post("/user", (req, res) -> {
            User user = gson.fromJson(req.body(), User.class);
            service.create(user);
            res.status(201);
            return gson.toJson(new ErrorResponse("Usuário cadastrado com sucesso!"));
        });

        put("/user/:cpf", (req, res) -> {
            User user = gson.fromJson(req.body(), User.class);
            service.update(req.params(":cpf"), user);
            res.status(200);
            return gson.toJson(new ErrorResponse("Dados atualizados com sucesso!"));
        });

        delete("/user/:cpf", (req, res) -> {
            service.remove(req.params(":cpf"));
            res.status(200);
            return gson.toJson(new ErrorResponse("Usuário removido com sucesso!"));
        });
    }
}