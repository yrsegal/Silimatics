package wiresegal.silimatics.common.item

import com.google.common.collect.Lists
import com.google.common.collect.Multimap
import com.teamwizardry.librarianlib.client.util.TooltipHelper
import com.teamwizardry.librarianlib.common.base.item.IItemColorProvider
import com.teamwizardry.librarianlib.common.base.item.ItemMod
import net.minecraft.entity.Entity
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import wiresegal.silimatics.api.lens.ILens
import wiresegal.silimatics.common.core.ModCreativeTab
import wiresegal.silimatics.common.item.ItemLensFrames.Companion.getLensStack
import wiresegal.silimatics.common.lens.*
import wiresegal.silimatics.common.lib.LibMisc
import wiresegal.silimatics.common.networking.MessageSmedrize
import wiresegal.silimatics.common.networking.NetworkHelper

class ItemLens(name: String) : ItemMod(name, *EnumSandType.getSandTypeNamesFor(name)), ILens, IItemColorProvider {

    init {
        MinecraftForge.EVENT_BUS.register(EventHandler)
        ModCreativeTab.set(this)
    }

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, playerIn: EntityPlayer, tooltip: MutableList<String>, advanced: Boolean) {
        val tempTip = Lists.newArrayList<String>()
        getLensFromStack(stack).addTooltip(stack, playerIn, tempTip, advanced)
        if (tempTip.size > 0)
            TooltipHelper.tooltipIfShift(tooltip) { tooltip.addAll(tempTip) }
    }

    override fun addTooltip(stack: ItemStack, playerIn: EntityPlayer, tooltip: MutableList<String>, advanced: Boolean) {
        val tempTip = Lists.newArrayList<String>()
        getLensFromStack(stack).addTooltip(stack, playerIn, tempTip, advanced)
        if (tempTip.size > 0)
            TooltipHelper.tooltipIfShift(tooltip) { tooltip.addAll(tempTip) }
    }

    override fun onUsingTick(world: World, player: EntityPlayer, stack: ItemStack) {
        getLensFromStack(stack).onUsingTick(world, player, stack)
    }

    override fun onDeadTick(world: World, player: EntityPlayer, stack: ItemStack) {
        getLensFromStack(stack).onDeadTick(world, player, stack)
    }

    override fun onCleanupTick(world: World, entity: Entity, stack: ItemStack) {
        getLensFromStack(stack).onCleanupTick(world, entity, stack)
    }

    override fun shouldMarkAsOculator(stack: ItemStack): Boolean {
        return getLensFromStack(stack).shouldMarkAsOculator(stack)
    }

    override fun addAttributes(slot: EntityEquipmentSlot, stack: ItemStack, modifiers: Multimap<String, AttributeModifier>) {
        getLensFromStack(stack).addAttributes(slot, stack, modifiers)
    }

    override val itemColorFunction: ((ItemStack, Int) -> Int)?
        get() = { stack, tintIndex -> EnumSandType.values()[stack.itemDamage % EnumSandType.values().size].glassColor }

    companion object {
        val OCULATOR_TAG = LibMisc.MODID + ":oculator"

        object EventHandler {
            val OCULATOR = LibMisc.MODID + ":oculator"

            @SubscribeEvent
            fun onPlayerTick(event: LivingEvent.LivingUpdateEvent) {
                if (event.entity !is EntityPlayer) return
                val player = event.entity as EntityPlayer
                val headStack = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD)
                if (headStack != null && headStack.item is ItemLensFrames) {
                    val lensStack = headStack.getLensStack()
                    if ((lensStack.item as ILens).shouldMarkAsOculator(lensStack)) {
                        player.entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).setBoolean(OCULATOR, true)
                        if (!player.worldObj.isRemote)
                            NetworkHelper.sendToAll(MessageSmedrize(player.uniqueID, true))

                    }
                }

            }
        }

        var lenses = arrayOf(LensDull, LensRashid, LensOculator,
                LensFirebringer, LensWindstormer(), LensVoidstormer, LensTracker,
                LensWarrior, LensTorturer, LensShocker, LensBestower, LensScholar, LensCourier, LensShowstopper, LensDisguiser)

        fun getLensFromStack(stack: ItemStack): ILens {
            return lenses[stack.metadata % lenses.size]
        }
    }
}
