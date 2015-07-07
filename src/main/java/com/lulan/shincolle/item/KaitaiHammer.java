package com.lulan.shincolle.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

public class KaitaiHammer extends BasicItem {
	
	public KaitaiHammer() {
		super();
		this.setUnlocalizedName("KaitaiHammer");
		this.maxStackSize = 1;
		this.hasSubtypes = false;
		this.setMaxDamage(12);
	}
	
	//����l�i�Ω�X����L�D��, �B�������ӫ~, �GContainerItem���@�[��-1���ۤv����
	@Override
	public boolean hasContainerItem(ItemStack stack) {
		return true;
	}
	
	@Override
	public ItemStack getContainerItem(ItemStack stack) {
		int meta = stack.getItemDamage() + 1;
		
		stack.setItemDamage(meta);	//�@�[��--
		
		if(meta >= this.getMaxDamage()) {	//���~�F��@�[�פW��, �^�ǪŪ��~
			return null;
		}
		
		return stack;
	}
	
	@Override
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack stack) {
        return false;	//�X���ᦹ���~�|�~��d�b�X���x��
    }
	
	//�PgetUnlocalizedName() �����[�Witemstack����
	//�榡��item.MOD�W��:���~�W��.name
	@Override
	public String getUnlocalizedName(ItemStack itemstack) {
		return String.format("item.%s", Reference.MOD_ID+":KaitaiHammer");
	}
	
	//����Ω�ۤv����ĥ, �i�ϸӴ�ĥ�@�����` (��^���~���A, ����-1)
	@Override
	public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
		//entity is ship
		if(entity instanceof BasicEntityShip) {
			//player is owner
			if(EntityHelper.checkSameOwner(player, entity)) {
				entity.attackEntityFrom(DamageSource.causePlayerDamage(player), ((BasicEntityShip) entity).getMaxHealth() * 1.01F);
				
				//item meta+1
				int meta = stack.getItemDamage()+1;
				
				if(meta >= stack.getMaxDamage()) {
					//destroy the hammer
					if(player.inventory.getCurrentItem() != null && 
					   player.inventory.getCurrentItem().getItem() == ModItems.KaitaiHammer) {
						player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
					}
				}
				else {
					stack.setItemDamage(meta);
				}
			}
		}
		
        return false;
    }
	
	
	
	
	
}
