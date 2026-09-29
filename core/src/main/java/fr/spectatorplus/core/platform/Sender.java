package fr.spectatorplus.core.platform;

/**
 * Expéditeur d'une commande ou destinataire d'un message : joueur ou console.
 */
public interface Sender {

    String getName();

    boolean hasPermission(String permission);

    /** Message avec codes couleur « § ». Plusieurs lignes séparées par « \n » sont acceptées. */
    void sendMessage(String message);
}
