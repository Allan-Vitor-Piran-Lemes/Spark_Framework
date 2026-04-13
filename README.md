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

### 🛠️ Passo 1: Preparando a Estrutura e Dependências

No seu projeto, o arquivo principal de configuração é o `pom.xml`. É nele que dizemos ao Maven para baixar o Spark e o Gson. Adicione isto dentro do seu `pom.xml`:

```xml
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
</dependencies>  ```

Sua estrutura de pastas em src/main/java/org/example deve ficar assim:

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
O Model é a planta-baixa. Ele define as características da nossa entidade "Pessoa", incluindo seus dados pessoais e endereço.
Abra o arquivo model/User.java e crie a classe:

Java
package org.example.model;

public class User {
    private String name;
    private String cpf; // Nosso identificador único
    private String phone;
    private String street;
    private String number;
    private String neighborhood;
    private String zipCode;
    private String city;
    private String state;

    // O Gson exige um construtor vazio para conseguir traduzir o JSON de volta para Java
    public User() {}

    // Nota: Gere os Getters e Setters na sua IDE (Alt+Insert) para todos os campos!
}
🗄️ Passo 3: O Repositório (Simulando o Banco)
Para focar no framework, usaremos uma List estática na memória. Quando o servidor desligar, os dados somem, mas funciona perfeitamente para entender o fluxo do CRUD.
Abra o arquivo repository/UserRepository.java:

Java
package org.example.repository;

import org.example.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    // Lista estática que simula nosso banco de dados.
    private static final List<User> users = new ArrayList<>();

    public void save(User user) {
        users.add(user);
    }

    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    // Busca um usuário pelo CPF. Retorna um 'Optional' para evitar erros caso não ache.
    public Optional<User> findByCpf(String cpf) {
        return users.stream().filter(u -> u.getCpf().equals(cpf)).findFirst();
    }

    public void update(User user) {
        delete(user.getCpf()); // Apaga o antigo
        save(user); // Salva o novo
    }

    public void delete(String cpf) {
        users.removeIf(u -> u.getCpf().equals(cpf));
    }
}
💼 Passo 4: As Regras de Negócio (Service)
Nunca deixe o "Garçom" (Controller) decidir as regras do restaurante. Essa é a função do Service. Ele valida as ações antes de falar com o Repositório.
Abra o arquivo service/UserService.java:

Java
package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;
import java.util.List;

public class UserService {
    private final UserRepository repository = new UserRepository();

    public void create(User user) {
        // Regra: Não pode existir dois CPFs iguais!
        if (repository.findByCpf(user.getCpf()).isPresent()) {
            throw new RuntimeException("Erro: Este CPF já está cadastrado no sistema.");
        }
        repository.save(user);
    }

    public List<User> getAll() {
        return repository.findAll();
    }

    public User getByCpf(String cpf) {
        // Se não achar o CPF, "joga" um erro para cima, que o Main vai capturar depois.
        return repository.findByCpf(cpf)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no sistema."));
    }

    public void update(String cpf, User user) {
        getByCpf(cpf); // Chama apenas para validar se o usuário existe
        user.setCpf(cpf); // Força que o CPF continue o mesmo da URL, ignorando o do JSON
        repository.update(user);
    }

    public void remove(String cpf) {
        getByCpf(cpf); // Valida se existe antes de tentar apagar
        repository.delete(cpf);
    }
}
🚦 Passo 5: As Rotas da API (Controller)
Aqui é onde o Spark brilha! Em poucas linhas, declaramos os endereços (URLs) que nossa API vai responder.

🧠 Conceitos Importantes Desta Classe:
req e res: Toda rota recebe uma Requisição (req - o que o usuário enviou) e devolve uma Resposta (res - o que vamos entregar).

req.body() e req.params(): O .body() pega o JSON gigantesco que o usuário enviou. O .params(":cpf") pega aquele número que o usuário digitou lá na URL (ex: /user/1111).

CORS (A linha mágica): Navegadores bloqueiam chamadas de edição (PUT) e deleção (DELETE) por segurança. As configurações de options e before servem para "abrir a porta" e avisar que nossa API é segura.

Abra o arquivo controller/UserController.java:

Java
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
        // Liberação de CORS para permitir testar a API no navegador (Scalar/Swagger)
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

        // ================= ROTAS DO CRUD =================

        // LISTAR (GET) -> Converte a lista do banco para JSON direto
        get("/users", (req, res) -> service.getAll(), gson::toJson);

        // BUSCAR UM (GET) -> Pega o :cpf da URL
        get("/user/:cpf", (req, res) -> service.getByCpf(req.params(":cpf")), gson::toJson);

        // CRIAR (POST) -> Transforma o Texto (JSON) em um Objeto Java (User.class)
        post("/user", (req, res) -> {
            User user = gson.fromJson(req.body(), User.class);
            service.create(user);
            res.status(201); // 201 significa "Criado com sucesso"
            return "Usuário cadastrado com sucesso!";
        });

        // ATUALIZAR (PUT)
        put("/user/:cpf", (req, res) -> {
            User user = gson.fromJson(req.body(), User.class);
            service.update(req.params(":cpf"), user);
            res.type("application/json");
            return gson.toJson(new ErrorResponse("Dados atualizados com sucesso!"));
        });

        // DELETAR (DELETE)
        delete("/user/:cpf", (req, res) -> {
            service.remove(req.params(":cpf"));
            res.type("application/json");
            return gson.toJson(new ErrorResponse("Usuário removido com sucesso!"));
        });
    }
}
(Nota: Para padronizar as mensagens, você pode criar uma classe simples ErrorResponse.java apenas com um atributo String message para formatar os avisos de sucesso/erro).

🚪 Passo 6: O Ponto de Entrada (Main)
Com a arquitetura pronta, o nosso Main.java fica super limpo. Ele só precisa ligar o servidor, chamar as rotas e dizer o que fazer caso algum erro aconteça no meio do caminho.

Abra o arquivo Main.java:

Java
package org.example;

import org.example.controller.UserController;
import org.example.exception.ErrorResponse;
import com.google.gson.Gson;
import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        // 1. Define em qual porta o servidor vai rodar
        port(4567);

        // 2. Chama o nosso Garçom para anotar os pedidos (Rotas)
        UserController userController = new UserController();
        userController.routes();

        Gson gson = new Gson();

        // 3. O "Pára-quedas": Se em qualquer lugar do código der um erro (RuntimeException),
        // ele cai aqui, devolve o Status 400 (Bad Request) e formata a mensagem de erro em JSON.
        exception(RuntimeException.class, (ex, req, res) -> {
            res.status(400);
            res.type("application/json");
            res.body(gson.toJson(new ErrorResponse(ex.getMessage())));
        });

        System.out.println("🚀 Servidor rodando em http://localhost:4567");
    }
}

✅ Pronto para Testar!
Rode a classe Main na sua IDE (IntelliJ).

O servidor subirá instantaneamente.

Para testar, você não conseguirá usar apenas o navegador (pois ele só faz requisições GET). Você precisará utilizar ferramentas como Postman, Insomnia ou uma documentação interativa gerada no Scalar (com um arquivo OpenAPI/Swagger) para enviar os JSONs para o seu código!
