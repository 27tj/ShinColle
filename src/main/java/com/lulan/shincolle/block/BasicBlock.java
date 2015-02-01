package com.lulan.shincolle.block;

import com.lulan.shincolle.creativetab.CreativeTabSC;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;

public class BasicBlock extends Block {

	//���w�������block
	public BasicBlock(Material material) {
		super(material);
		this.setCreativeTab(CreativeTabSC.SC_TAB);	//�[�J��creative tab��
	}
	
	//�L���w������ �w�]��rock��
	public BasicBlock() {
		this(Material.rock);
		this.setCreativeTab(CreativeTabSC.SC_TAB);	//�[�J��creative tab��
	}
	
	//name�]�w�Τ�k: �N�쥻mc����block�W�� �h��.���e���r�� �H�K�t�~��Wmod�W�٧Φ����r��
	protected String getUnwrappedUnlocalizedName(String unlocalizedName) {
		return unlocalizedName.substring(unlocalizedName.indexOf(".")+1);
	}
	
	//�Nname�a�Wmod�W�� �Ω󤧫ᵹ�U�y�t�ɮש�W���T�W��
	//�榡��tile.MOD�W��:����W��.name
	@Override
	public String getUnlocalizedName() {
		return String.format("tile.%s%s", Reference.MOD_ID+":", getUnwrappedUnlocalizedName(super.getUnlocalizedName()));
	}
	
	//����ϥܵn��
	//���X����W��(���tmod�W��)�@���Ѽƥᵹicon register�ӵn��icon
	//�`�Nicon�u�bclient�ݤ~�ݭn����
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
		blockIcon = iconRegister.registerIcon(String.format("%s", getUnwrappedUnlocalizedName(this.getUnlocalizedName())));
	}

}
