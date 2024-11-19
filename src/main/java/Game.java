import gigaChat.GigaChatDialog;

public class Game {
    public static void main(String[] args) {
        GigaChatDialog gigaChatDialog = new GigaChatDialog();
        String response = gigaChatDialog.getResponse("Привет, GigaChat! Как дела?");
        System.out.println("GigaChat: " + response);
    }
}
