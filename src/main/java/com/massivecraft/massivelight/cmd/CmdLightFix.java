package com.massivecraft.massivelight.cmd;

import org.bukkit.Chunk;
import org.bukkit.World;

import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeInteger;
import com.massivecraft.massivelight.ChunkWrap;
import com.massivecraft.massivelight.Perm;
import com.massivecraft.massivelight.entity.MConf;

public class CmdLightFix extends MassiveCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdLightFix()
	{
		// Aliases
		this.addAliases("fix");
		
		// Parameters
		this.addParameter(TypeInteger.get(), "radius", "0");
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.FIX));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		int radius = this.readArg(0);
		if (radius < 0)
		{
			msg("<b>Radius may not be a negative value.");
			return;
		}
		if (radius > MConf.get().maxradius)
		{
			msg("<b>The maxium radius allowed is <h>%d<b>.", MConf.get().maxradius);
			return;
		}
		
		Chunk origin = me.getLocation().getChunk();
		int originX = origin.getX();
		int originZ = origin.getZ();
		World world = me.getWorld();
		
		// Pre Inform
		int side = (1 + radius * 2);
		int target = side * side;
		msg("<i>Chunks around you will now be relighted.");
		msg("<k>Radius <v>%d <a>--> <k>Side <v>%d <a>--> <k>Chunks <v>%d", radius, side, target);
		
		// Apply
		for (int deltaX = -radius; deltaX <= radius; deltaX++)
		{
			for (int deltaZ = -radius; deltaZ <= radius; deltaZ++)
			{
				int x = originX + deltaX;
				int z = originZ + deltaZ;
				new ChunkWrap(world, x, z).recalcLightLevel();
			}	
		}
		
		// Post Inform
		msg("<g>Chunk relight complete.");
	}
	
}
