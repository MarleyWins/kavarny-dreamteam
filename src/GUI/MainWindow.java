/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI;

import DB.Database;
import DB.DatabaseGetters;
import GUI.forms.Cafes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import kavarny_dreamteam.Main;
import kavarny_dreamteam.User;

/**
 * Hlavní třída, která se zobrazí po přihlášení obsahuje kromě formulářů všechna
 * pohledová okna nastavuje nástrojové postranní lišty
 *
 * @author rostaklein
 */
public class MainWindow {

    private final BorderPane borderPane;
    private final Main main;
    private final ScrollPane scroll;
    private final Label message;
    private VBox kavarnyList;
    private ArrayList<kavarny_dreamteam.Cafes> cafeList;

    /**
     * Inicializuje třídu, nastaví jednotlivé části screenu, pokud uživatel není přihlášen, nastaví přihlašovací obrazovku.
     * @param main
     */
    public MainWindow(Main main) {

        this.main = main;
        borderPane = new BorderPane();
        borderPane.setBackground(main.getBgImage());

        scroll = new ScrollPane();
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        scroll.setFitToWidth(true);

        cafeList = new DatabaseGetters().getAllCafes();
        kavarnyList = new KavarnyList(cafeList, this, main, "");

        message = new Label();
        message.setText("");
        message.setAlignment(Pos.CENTER_LEFT);

        // pokud existuje přihlášený user, má cenu vytvářet dashboard
        if (main.getSignedUser() != null) {
            scroll.setContent(kavarnyList);
            borderPane.setCenter(scroll);
            borderPane.setTop(createTopBar());
            borderPane.setLeft(createLeftBar());
        }
    }

    /**
     * Updatuje list kaváren (voláno po změně)
     */
    private void updateCafes(){
        cafeList = new DatabaseGetters().getAllCafes();
        kavarnyList = new KavarnyList(cafeList, this, main, "");
    }

    /**
     * vrací celý main window
     * @return hlavní BorderPane, ve kterém je main window
     */
    public BorderPane getContent() {
        return borderPane;
    }

    /**
     * Slouží k přenastavení hlavní content area
     * @return hlavní content area
     */
    public ScrollPane getScroll() {
        return scroll;
    }

    /**
     * vytváří horní lištu s názvem aplikace a jménem přihlášeného uživatele
     * @return horní lišta se signed userem, signout buttonem a button pro požádání o správce
     */
    private HBox createTopBar() {
        //horizontální zobrazení
        HBox hbox = new HBox(20);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPrefHeight(main.WINDOW_HEIGHT / 10);

        Text caption = new Text();
        //Text userEmail = new Text();

        caption.setText("Vítejte, "+main.getSignedUser().getEmail());
        caption.setFont(Font.font("Helvetica", FontWeight.BOLD, 16));
        caption.setFill(Paint.valueOf("white"));

        Button signOut = new Button("Odhlásit");

        signOut.setOnAction(event -> {
            main.getSuperUser().setSigned(false);
            main.signOutCurrentUser();
            main.setScene(new Scene(new LoginForm(main).getContent()));
        });

        hbox.getChildren().add(caption);

        //pokud ještě nepožádal o správcování a není admin
        if (!main.getSignedUser().getWantsToBeAdmin() && !main.getSignedUser().isAdmin()) {
            Button requestAdmin = new Button("Žádost o správcování");
            requestAdmin.setOnAction(event ->
                getWantsToBeAdminWindow()
            );
            hbox.getChildren().add(requestAdmin);
        }

        hbox.getChildren().add(signOut);

        hbox.setPadding(new Insets(0, 0, 0, 10));
        return hbox;
    }

    /**
     * Funkce vybírá z databáze všechny uživatele, kteří požadují adminská práva.
     * Je zobrazena pouze superadminovi a ten může kliknutím na tlačítko potvrdit.
     * @return panel s requesty o admin práva
     */
    private FlowPane showAdminRequests() {
        FlowPane flowPane = new FlowPane();
        Text heading = new Text("Seznam žádostí o správcování");
        heading.setFont(Font.font("Helvetica", FontWeight.BOLD, 14));
        flowPane.getChildren().addAll(heading);
        flowPane.setPadding(new Insets(5, 5, 5, 10));
        ArrayList<User> users = new ArrayList<>();
        try {
            ResultSet res = Database.getPrepStatement("SELECT * from users where wantsToBeAdmin=1").executeQuery();
            while(res.next()){
                users.add(new User(res.getInt("id"), res.getString("email"), res.getBoolean("admin"), res.getBoolean("wantsToBeAdmin")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        GridPane grid = new GridPane();

        grid.setPrefWidth((main.WINDOW_WIDTH / 6) * 5);
        grid.setPadding(new Insets(10, 10, 5, 0));

        grid.add(new Text("id uživatele"), 0, 0);
        grid.add(new Text("email uživatele"), 1, 0);
        grid.add(new Text("potvrdit možnost správcování"), 2, 0);

        int iterator = 1;

        for (User user : users) {
            Button confirm = new Button("Potvrdit");
            confirm.setOnAction(event -> {
                try {
                    Database.getPrepStatement("UPDATE users set admin=1, wantsToBeAdmin=0 where id="+user.getId()).executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            confirm.setId(Integer.toString(user.getId()));
            Text id = new Text(Integer.toString(user.getId()));
            Text email = new Text(user.getEmail());

            grid.add(id, 0, iterator);
            grid.add(email, 1, iterator);
            grid.add(confirm, 2, iterator);
            grid.setHgap(10);
            grid.setVgap(10);

            iterator++;
        }

        flowPane.getChildren().addAll(grid);
        return flowPane;
    }

    /**
     * levá zobrazovací lišta obsahující navigaci pro uživatele
     * Některé položky jsou viditelné všem, některé pouze adminovi/superadminovi.
     * @return levá nástrojová lišta GridPane
     */
    private GridPane createLeftBar() {
        GridPane grid = new GridPane();

        double gridWidth = main.WINDOW_WIDTH / 6;
        grid.setPrefWidth(gridWidth);
        grid.setPadding(new Insets(10, 10, 10, 10));

        Button welcome = new Button("Všechny\nkavárny");
        welcome.setTextAlignment(TextAlignment.CENTER);
        welcome.setPrefWidth(gridWidth);
        welcome.setOnAction(event ->{
            scroll.setContent(null);
            updateCafes();
            scroll.setContent(kavarnyList);
        });

        if (main.getSignedUser().isAdmin()) {
            Button button = new Button("Přidat\nkavárnu");
            button.setTextAlignment(TextAlignment.CENTER);
            //nastaví Cafes formu null = přidání nové kavárny
            button.setPrefWidth(gridWidth);
            button.setOnAction((event -> scroll.setContent(new Cafes(null, main))));
            grid.add(button, 0, 2);
        }

        if (main.getSignedUser().isSuperAdmin()) {
            Button button = new Button("Žádosti\no správcování");
            button.setOnAction(event -> scroll.setContent(showAdminRequests()));
            button.setTextAlignment(TextAlignment.CENTER);
            button.setPrefWidth(gridWidth);
            grid.add(button, 0, 3);
        }

        grid.add(welcome, 0, 1);

        grid.add(getSearchNameBox(gridWidth), 0, 4);

        grid.setVgap(10);

        return grid;
    }

    /**
     * Vytváří search input a button pro vyhledávání v kavárnách dle jména.
     * @param gridWidth pro nastavení šířky search boxu
     * @return searchbox s buttonem pro vyhledávání v kavárnách
     */
    private VBox getSearchNameBox(Double gridWidth){
        //new KavarnyList(cafeList, this, main)
        VBox search = new VBox();
        TextField searchBy = new TextField();
        searchBy.setPromptText("Název kavárny");
        Button submit = new Button("Vyhledat");
        submit.setTextAlignment(TextAlignment.CENTER);
        searchBy.setAlignment(Pos.CENTER);
        searchBy.setPrefWidth(gridWidth);
        submit.setPrefWidth(gridWidth);
        submit.setOnAction(event -> searchByName(searchBy.getText()));
        searchBy.setOnAction(event -> searchByName(searchBy.getText()));
        searchBy.setPadding(new Insets(5));
        search.setSpacing(2);
        search.getChildren().setAll(searchBy, submit);
        return search;
    }

    /**
     * Hledání dle jména.
     * @param value jaký text se vyskytuje v názvu kavárny
     */
    private void searchByName(String value){
        System.out.println("Hledám kavárnu podle: "+value);
        scroll.setContent(new KavarnyList(new DatabaseGetters().findCafeByName(value), this, main, "Výsledky hledání kavárny: '"+value+"'"));
    }


    /**
     * Vyskakovací okno s admin právy.
     * Ptá se, zda chce uživatel skutečně požádat, případně pokud už požádal tak mu to sdělí.
     */
    public void getWantsToBeAdminWindow(){
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Žádost o správce");
        //uživatel již požádal o možnost správcování
        if (main.getSignedUser().getWantsToBeAdmin()) {
            alert.setHeaderText("Již jste požádali o možnost správcování");
            alert.setContentText("Vyčkejte, dokud hlavní admin neschválí vaši žádost");
            alert.showAndWait();
        } //uživatel již má možnost správcování
        else if (main.getSignedUser().getWantsToBeAdmin() && main.getSignedUser().isAdmin()) {
            alert.setHeaderText("Již máte možnost správcování");
            alert.showAndWait();
        } //pokud uživatel stiskne OK, do db se propíše žádost o správcování
        else {
            alert.setHeaderText("Tímto požádáte o možnost správcování");
            alert.setContentText("Opravdu se chcete stát správcem?");
            Optional<ButtonType> result = alert.showAndWait();

            if (result.get() == ButtonType.OK) {
                Database db = Database.getInstance();
                PreparedStatement preparedStatement = Database.getPrepStatement("UPDATE users SET wantsToBeAdmin=1 where id=?");
                try {
                    if (preparedStatement != null) {
                        preparedStatement.setInt(1, main.getSignedUser().getId());
                        preparedStatement.executeUpdate();
                        System.out.println("Uzivatel "+main.getSignedUser().getEmail()+" prave pozadal o opravneni.");
                        main.getSignedUser().setWantsToBeAdmin(true);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
