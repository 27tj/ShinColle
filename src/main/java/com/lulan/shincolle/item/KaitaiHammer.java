package com.lulan.shincolle.item;

import com.lulan.shincolle.reference.Reference;

import net.minecraft.item.ItemStack;

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
	
	
	
	
	
}
