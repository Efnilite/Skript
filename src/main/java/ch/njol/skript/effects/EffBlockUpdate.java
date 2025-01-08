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
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

@Name("Update Block")
@Description({
	"Updates the blocks by setting them to a selected block",
	"Using 'without physics' will not send updates to the surrounding blocks of the blocks being set.",
	"Example: Updating a block next to a sand block in the air 'without physics' will not cause the sand block to fall."
})
@Examples({
	"update {_blocks::*} as gravel",
	"update {_blocks::*} to be sand without physics updates",
	"update {_blocks::*} as stone without neighbouring updates"
})
@Since("2.10")
// Originally sourced from SkBee by ShaneBee (https://github.com/ShaneBeee/SkBee/blob/master/src/main/java/com/shanebeestudios/skbee/elements/other/effects/EffBlockstateUpdate.java)
public class EffBlockUpdate extends Effect implements SyntaxRuntimeErrorProducer {

	static {
		Skript.registerEffect(EffBlockUpdate.class,
			"update %blocks% (as|to be) %blockdata% [physics:without [neighbo[u]r[ing]|adjacent] [physic[s]] update[s]]");
	}

	private Node node;
	private boolean physics;
	private Expression<Block> blocks;
	private Expression<BlockData> blockData;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		this.node = getParser().getNode();
		this.physics = !parseResult.hasTag("physics");
		this.blocks = (Expression<Block>) exprs[0];
		this.blockData = (Expression<BlockData>) exprs[1];
		return true;
	}

	@Override
	protected void execute(Event event) {
		BlockData data = this.blockData.getSingle(event);
		if (data == null) {
			error("The blockdata to update the block(s) to was null.", this.blockData.toString(null, false));
			return;
		}

		for (Block block : this.blocks.getArray(event)) {
			block.setBlockData(data, this.physics);
		}
	}

	@Override
	public Node getNode() {
		return node;
	}

	@Override
	public @NotNull String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug)
			.append("update", blocks, "as", blockData);
		if (physics)
			builder.append("without adjacent updates");
		return builder.toString();
	}

}
