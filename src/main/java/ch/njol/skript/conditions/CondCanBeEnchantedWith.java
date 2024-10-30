package ch.njol.skript.conditions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.conditions.base.PropertyCondition.PropertyType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name("Can Be Enchanted With")
@Description({
	"Checks whether an item has enchantments that conflict with the given enchantment.",
	"For example, a sword cannot have both sharpness and smite. " +
	"Therefore, sharpness conflicts with smite, so a sword with sharpness cannot be enchanted with smite.",
})
@Examples({
	"player's tool can be enchanted with efficiency",
	"event-item can be enchanted with power"
})
@Since("INSERT VERSION")
public class CondCanBeEnchantedWith extends Condition {
	
	static {
		PropertyCondition.register(CondCanBeEnchantedWith.class, PropertyType.CAN,
				"be enchanted with [:stored] %enchantments%", "itemtypes");
	}

	private Expression<ItemType> items;
	private Expression<Enchantment> enchantment;
	private boolean isStored;
	
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern,
						Kleenean isDelayed, ParseResult parseResult) {
		isStored = parseResult.hasTag("stored");
		//noinspection unchecked
		items = (Expression<ItemType>) expressions[0];
		//noinspection unchecked
		enchantment = (Expression<Enchantment>) expressions[1];
		setNegated(matchedPattern == 1);
		return true;
	}
	
	@Override
	public boolean check(Event event) {
		Enchantment enchantment = this.enchantment.getSingle(event);
		if (enchantment == null)
			return false;

		return items.check(event, item -> {
			ItemMeta meta = item.getItemMeta();
			if (isStored && meta instanceof EnchantmentStorageMeta enchantmentMeta) {
				return enchantmentMeta.hasConflictingStoredEnchant(enchantment);
			} else {
				return meta.hasConflictingEnchant(enchantment);
			}
		}, isNegated());
	}
	
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return PropertyCondition.toString(this, PropertyType.HAVE, event, debug, items,
			"conflicting " + (isStored ? "stored " : "") + "enchantments with " + enchantment.toString(event, debug));
	}
	
}
