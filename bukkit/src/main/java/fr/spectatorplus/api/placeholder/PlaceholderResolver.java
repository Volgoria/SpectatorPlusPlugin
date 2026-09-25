package fr.spectatorplus.api.placeholder;

/**
 * Calcule dynamiquement la valeur d'un placeholder au moment de son utilisation.
 * <pre>
 * api.getPlaceholderService().register("role", ctx -&gt; roles.get(ctx.getSubjectId()));
 * // utilisable ensuite partout : "{player} est {role}"
 * </pre>
 */
public interface PlaceholderResolver {

    /** @return la valeur, ou null si le placeholder ne s'applique pas dans ce contexte */
    String resolve(PlaceholderContext context);
}
