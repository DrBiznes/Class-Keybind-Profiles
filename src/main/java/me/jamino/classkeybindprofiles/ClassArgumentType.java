package me.jamino.classkeybindprofiles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.wynntils.models.character.type.ClassType;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ClassArgumentType implements ArgumentType<ClassType> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Warrior", "Mage", "Archer", "Assassin", "Shaman");
    private static final DynamicCommandExceptionType INVALID_CLASS = new DynamicCommandExceptionType(
            className -> Text.literal("Invalid class name: " + className)
    );

    public static ClassArgumentType classType() {
        return new ClassArgumentType();
    }

    @Override
    public ClassType parse(StringReader reader) throws CommandSyntaxException {
        String className = reader.readUnquotedString();
        try {
            // Convert to title case for display (e.g., "warrior" -> "Warrior")
            String titleCaseClassName = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();

            // But use uppercase for enum parsing
            ClassType classType = ClassType.valueOf(className.toUpperCase());
            return classType != ClassType.NONE ? classType : null;
        } catch (IllegalArgumentException e) {
            throw INVALID_CLASS.create(className);
        }
    }

    public static ClassType getClassType(CommandContext<?> context, String name) {
        return context.getArgument(name, ClassType.class);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (ClassType type : ClassType.values()) {
            if (type != ClassType.NONE) {
                // Suggest the properly capitalized class name
                String properName = type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase();
                if (properName.toLowerCase().startsWith(remaining)) {
                    builder.suggest(properName);
                }
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}