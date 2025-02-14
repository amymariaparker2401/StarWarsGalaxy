package com.parzivail.swg.player;

import com.parzivail.swg.Resources;
import com.parzivail.swg.StarWarsGalaxy;
import com.parzivail.swg.force.ForcePowerDescriptor;
import com.parzivail.swg.force.IForcePower;
import com.parzivail.swg.network.client.MessagePswgExtPropSync;
import com.parzivail.swg.player.species.SpeciesType;
import com.parzivail.util.item.NbtSave;
import com.parzivail.util.item.NbtSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants.NBT;
import org.apache.commons.lang3.ArrayUtils;

import java.util.UUID;

public class PswgExtProp extends NbtSerializable<PswgExtProp> implements IExtendedEntityProperties
{
	public static final String PROP_NAME = Resources.MODID + "_eep";

	@NbtSave
	protected int creditBalance;
	@NbtSave
	protected int[] unlockedBlasterAttachments;
	@NbtSave
	protected String[] activeQuests;
	@NbtSave
	protected String[] completedQuests;
	@NbtSave
	protected int species;
	@NbtSave
	protected boolean changingDimensions;
	@NbtSave
	protected UUID shipRiding;
	@NbtSave
	protected int shipRidingSeatIdx;
	@NbtSave
	protected String[] flags;
	@NbtSave
	protected ForcePowerDescriptor[] forcePowers = new ForcePowerDescriptor[0];

	private Entity entity;
	private World world;

	public static void register()
	{
		MinecraftForge.EVENT_BUS.register(new PswgExtPropHandler());
	}

	public static PswgExtProp get(Entity p)
	{
		if (p == null)
			return null;
		return (PswgExtProp)p.getExtendedProperties(PROP_NAME);
	}

	public boolean isBlasterAttachmentUnlocked(int attachmentId)
	{
		return ArrayUtils.contains(unlockedBlasterAttachments, attachmentId);
	}

	public boolean hasQuest(String quest)
	{
		return ArrayUtils.contains(activeQuests, quest) || ArrayUtils.contains(completedQuests, quest);
	}

	public boolean hasFlag(String flag)
	{
		return ArrayUtils.contains(flags, flag);
	}

	public SpeciesType getSpecies()
	{
		return SpeciesType.values()[species];
	}

	public void setSpecies(SpeciesType species)
	{
		this.species = species.ordinal();
		sync();
	}

	public int getCreditBalance()
	{
		return creditBalance;
	}

	public void setCreditBalance(int creditBalance)
	{
		this.creditBalance = creditBalance;
		sync();
	}

	public void addCreditBalance(int delta)
	{
		creditBalance += delta;
		sync();
	}

	public void unlockBlasterAttachment(int attachmentId)
	{
		unlockedBlasterAttachments = ArrayUtils.add(unlockedBlasterAttachments, attachmentId);
		sync();
	}

	public void startQuest(String quest)
	{
		activeQuests = ArrayUtils.add(activeQuests, quest);
		sync();
	}

	public void completeQuest(String quest)
	{
		activeQuests = ArrayUtils.removeElement(activeQuests, quest);
		completedQuests = ArrayUtils.add(completedQuests, quest);
		sync();
	}

	public void setFlag(String flag)
	{
		if (hasFlag(flag))
			return;
		flags = ArrayUtils.add(flags, flag);
		sync();
	}

	public void clearFlag(String flag)
	{
		flags = ArrayUtils.removeElement(flags, flag);
		sync();
	}

	public String[] getActiveQuests()
	{
		return activeQuests;
	}

	public String[] getCompletedQuests()
	{
		return completedQuests;
	}

	public String[] getFlags()
	{
		return flags;
	}

	public ForcePowerDescriptor[] getPowers()
	{
		return forcePowers;
	}

	public ForcePowerDescriptor getPower(IForcePower power)
	{
		// TODO: return null if not force sensitive
		ForcePowerDescriptor[] powers = getPowers();
		for (ForcePowerDescriptor descriptor : powers)
			if (descriptor.describes(power))
				return descriptor;
		ForcePowerDescriptor desc = new ForcePowerDescriptor(power);
		updatePower(desc);
		return desc;
	}

	public void updatePower(ForcePowerDescriptor descriptor)
	{
		// This simple remove-add works because the equality of two descriptors
		// is solely based on the ID of the described power
		forcePowers = ArrayUtils.removeElement(forcePowers, descriptor);
		forcePowers = ArrayUtils.add(forcePowers, descriptor);
		sync();
	}

	@Override
	public void init(Entity entity, World world)
	{
		this.entity = entity;
		this.world = world;
	}

	@Override
	public void saveNBTData(NBTTagCompound compound)
	{
		NBTTagCompound data = new NBTTagCompound();
		serialize(data);
		compound.setTag(PROP_NAME, data);
	}

	@Override
	public void loadNBTData(NBTTagCompound compound)
	{
		if (!compound.hasKey(PROP_NAME, NBT.TAG_COMPOUND))
			return;

		NBTTagCompound data = compound.getCompoundTag(PROP_NAME);
		deserialize(data);
	}

	void sync()
	{
		if (world.isRemote)
			return;

		MessagePswgExtPropSync message = new MessagePswgExtPropSync((EntityPlayer)entity, this);
		StarWarsGalaxy.network.sendToDimension(message, entity.dimension);
	}

	void playerStartedTracking(EntityPlayer entityPlayer)
	{
		StarWarsGalaxy.network.sendTo(new MessagePswgExtPropSync((EntityPlayer)entity, this), (EntityPlayerMP)entityPlayer);
	}

	public UUID getShipRiding()
	{
		return shipRiding;
	}

	public void setShipRiding(UUID shipId)
	{
		shipRiding = shipId;
		sync();
	}

	public int getShipRidingSeatIdx()
	{
		return shipRidingSeatIdx;
	}

	public void setShipRidingSeatIdx(int idx)
	{
		shipRidingSeatIdx = idx;
		sync();
	}

	public void setChangingDimensions(boolean changingDimensions)
	{
		this.changingDimensions = changingDimensions;
		sync();
	}

	public boolean isChangingDimensions()
	{
		return changingDimensions;
	}
}
