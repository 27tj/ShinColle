package com.lulan.shincolle.creativetab;

import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.Reference;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

//�إ�mod��creative tab
public class CreativeTabSC {
		
	public static final CreativeTabs SC_TAB = new CreativeTabs(Reference.MOD_ID) {
		//tab��icon
		@Override
		public Item getTabIconItem() {
			return ModItems.Grudge;	//�Ψ䤤�@�Ӫ��~��icon��@creative tab��icon
		}	
		//tab��ܪ��W�ٷ|�۰ʧ�y�t�ɪ��r���J  �����ϥ�getTranslatedTabLabel
		//�u�n�b�y�t�ɤ��[�J  itemGroup.MOD�W��=�n��ܪ�tab�W��  �Y�i
	};

	

}
