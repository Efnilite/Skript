package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

@Name("Charge Entity")
@Description({
	"Charges or uncharges a creeper or wither skull.",
	"A creeper is charged when it has been struck by lightning."
})
@Examples({
	"on spawn of creeper:",
		"\tcharge the event-entity"
})
@Since("2.5, 2.10 (wither skulls)")
public class EffCharge extends Effect implements SyntaxRuntimeErrorProducer {

	static {
		Skript.registerEffect(EffCharge.class,
			"make %entities% [un:(un|not |non[-| ])](charged|powered)",
			"[:un](charge|power) %entities%");
	}

	private Node node;
	private Expression<Entity> entities;
	private boolean charge;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		node = getParser().getNode();
		entities = (Expression<Entity>) exprs[0];
		charge = !parseResult.hasTag("un");
		return true;
	}

	@Override
	protected void execute(Event event) {
		for (Entity entity : entities.getArray(event)) {
			if (entity instanceof Creeper creeper) {
				creeper.setPowered(charge);
			} else if (entity instanceof WitherSkull witherSkull) {
				witherSkull.setCharged(charge);
            } else {
				warning("An entity passed to the 'charge' effect wasn't a creeper nor wither skull, and is thus unaffected.", entities.toString(null, false));
			}
		}
	}

	@Override
	public Node getNode() {
		return node;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "make " + entities.toString(event, debug) + (charge ? " charged" : " not charged");
	}

}
