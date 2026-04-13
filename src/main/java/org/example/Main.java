package org.example;

import org.example.controller.UserController;
import org.example.exception.ErrorResponse; // Ele vai usar o da sua pasta certa
import com.google.gson.Gson;
import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        port(4567);

        UserController userController = new UserController();
        userController.routes();

        Gson gson = new Gson();

        // Tratamento de erro simples
        exception(RuntimeException.class, (ex, req, res) -> {
            res.status(400);
            res.type("application/json");
            // Usa a classe que está lá na pasta exception
            res.body(gson.toJson(new ErrorResponse(ex.getMessage())));
        });

        System.out.println("Servidor rodando em http://localhost:4567");
    }
}