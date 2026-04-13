# 🛠️ Tutorial: Construindo uma API CRUD Completa com Spark Framework (Java)

Neste tutorial, vamos construir o backend de um aplicativo para cadastro de **Pessoas/Usuários**. A nossa API permitirá Listar, Buscar, Criar, Editar e Apagar registros (operações clássicas de um CRUD) simulando um banco de dados.

Antes de colocar a mão no código, vamos entender alguns conceitos essenciais que utilizaremos neste projeto.

### 🧠 Conceitos Fundamentais

**1. Por que separar em vários pacotes (Arquitetura em Camadas)?**
Colocar todo o código no arquivo `Main.java` até funciona, mas vira uma bagunça rápida. Nós separamos o código em pacotes (`model`, `repository`, `service`, `controller`) para dividir as responsabilidades:
* **Model:** É o molde. Define o que é um Usuário (tem nome, cpf, etc.).
* **Repository:** É o bibliotecário. Só ele sabe como guardar e buscar os dados.
* **Service:** É o segurança/gerente. Ele aplica as regras de negócio (ex: "Não deixe cadastrar se o CPF já existir").
* **Controller:** É o garçom. Ele recebe o pedido da internet (ex: "Me dê a lista de usuários"), pede para o gerente (Service) e entrega a resposta para o cliente.

**2. O que é o Gson e o JSON?**
Sistemas diferentes (como o app em Flutter do seu cunhado e o seu backend em Java) não falam a mesma língua nativamente. O **JSON** (JavaScript Object Notation) é o "inglês" da programação: um formato de texto universal. A biblioteca **Gson**, do Google, pega os nossos objetos Java e os traduz automaticamente para JSON, permitindo que a internet os entenda.

**3. Spark e o Servidor Embutido**
Em Java tradicional, você precisaria baixar e configurar um servidor pesado (como o Tomcat). O Spark traz um servidor web **embutido** (o Jetty). Basta apertar o *Play* na classe Main e ele mesmo levanta o servidor.

---

🛠️ Passo 1: Preparando a Estrutura e Dependências
No seu projeto, o arquivo principal de configuração é o pom.xml. Adicione estas dependências para que o Maven baixe o Spark e o Gson:

XML
<dependencies>
    <dependency>
        <groupId>com.sparkjava</groupId>
        <artifactId>spark-core</artifactId>
        <version>2.9.4</version>
    </dependency>
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
</dependencies>
Sua estrutura de pastas deve refletir a organização abaixo:

Plaintext
 src/main/
 ├── java/org/example/
 │    ├── controller/
 │    │    └── UserController.java
 │    ├── exception/
 │    │    ├── ErrorResponse.java
 │    │    └── UserNotFoundException.java
 │    ├── model/
 │    │    └── User.java
 │    ├── repository/
 │    │    └── UserRepository.java
 │    ├── service/
 │    │    └── UserService.java
 │    └── Main.java
 └── resources/
      └── swagger.json

📦 Passo 2: O Modelo de Dados (Model)
O Model define a entidade "Pessoa" com seus dados e endereço. Abra o arquivo model/User.java:

Java
package org.example.model;

public class User {
    private String name;
    private String cpf; 
    private String phone;
    private String street;
    private String number;
    private String neighborhood;
    private String zipCode;
    private String city;
    private String state;

    public User() {}

    // Getters e Setters devem ser gerados aqui para todos os campos
}
🗄️ Passo 3: O Repositório (Simulando o Banco)
Utilizamos uma List estática em memória para armazenar os dados. Abra o arquivo repository/UserRepository.java:

Java
package org.example.repository;

import org.example.model.User;
import java.util.ArrayList;
import java.util.List;
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
        delete(user.getCpf());
        save(user);
    }

    public void delete(String cpf) {
        users.removeIf(u -> u.getCpf().equals(cpf));
    }
}

💼 Passo 4: Regras de Negócio (Service)
O Service valida as operações antes de persistir os dados. Abra o arquivo service/UserService.java:

Java
package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.exception.UserNotFoundException;
import java.util.List;

public class UserService {
    private final UserRepository repository = new UserRepository();

    public void create(User user) {
        if (repository.findByCpf(user.getCpf()).isPresent()) {
            throw new RuntimeException("Erro: Este CPF já está cadastrado.");
        }
        repository.save(user);
    }

    public List<User> getAll() {
        return repository.findAll();
    }

    public User getByCpf(String cpf) {
        return repository.findByCpf(cpf)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
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

🚦 Passo 5: As Rotas da API (Controller)
Aqui configuramos o CORS e os endereços HTTP. Abra o arquivo controller/UserController.java:

Java
package org.example.controller;

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
            if (headers != null) res.header("Access-Control-Allow-Headers", headers);
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            return "OK";
        });

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        });

        get("/users", (req, res) -> service.getAll(), gson::toJson);
        get("/user/:cpf", (req, res) -> service.getByCpf(req.params(":cpf")), gson::toJson);
        
        post("/user", (req, res) -> {
            User user = gson.fromJson(req.body(), User.class);
            service.create(user);
            res.status(201);
            return "Cadastrado com sucesso!";
        });

        put("/user/:cpf", (req, res) -> {
            User user = gson.fromJson(req.body(), User.class);
            service.update(req.params(":cpf"), user);
            return "Atualizado com sucesso!";
        });

        delete("/user/:cpf", (req, res) -> {
            service.remove(req.params(":cpf"));
            return "Removido com sucesso!";
        });
    }
}

🚪 Passo 6: Inicialização (Main)
A classe principal inicia o servidor e trata erros globais. Abra o arquivo Main.java:

Java
package org.example;

import org.example.controller.UserController;
import org.example.exception.ErrorResponse;
import com.google.gson.Gson;
import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        port(4567);

        UserController userController = new UserController();
        userController.routes();

        Gson gson = new Gson();

        exception(RuntimeException.class, (ex, req, res) -> {
            res.status(400);
            res.type("application/json");
            res.body(gson.toJson(new ErrorResponse(ex.getMessage())));
        });

        System.out.println("🚀 Servidor rodando em http://localhost:4567");
    }
}

✅ Pronto para Testar!
Execute a classe Main no IntelliJ.

Utilize o Scalar ou Postman para enviar requisições JSON.

Lembre-se que o CPF enviado na URL deve corresponder ao registro que você deseja manipular.
Para testar, você não conseguirá usar apenas o navegador (pois ele só faz requisições GET). Você precisará utilizar ferramentas como Postman, Insomnia ou uma documentação interativa gerada no Scalar (com um arquivo OpenAPI/Swagger) para enviar os JSONs para o seu código!
