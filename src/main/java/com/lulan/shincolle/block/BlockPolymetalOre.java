package com.lulan.shincolle.block;

import java.util.Random;

import com.lulan.shincolle.init.ModItems;

import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;

public class BlockPolymetalOre extends BasicBlock {
	
	public BlockPolymetalOre() {
	    super();
	    this.setBlockName("BlockPolymetalOre");
	    this.setHardness(3.0F);
	    this.setHarvestLevel("pickaxe", 2);
   
	}
	
	//���q�g��]�w
	//getExpDrop(IBlockAccess world, int metadata, int fortune)
	private Random rand = new Random();
	@Override
	public int getExpDrop(IBlockAccess world, int metadata, int fortune){
		if (this.getItemDropped(metadata, rand, fortune) != Item.getItemFromBlock(this)) { //���O���������ܵ��g��
			return (rand.nextInt(4)+1)*(fortune+1);	//��(���]����+1)x(1~4)���g��
		}
		return 0;	//���������� ��0
	}

	//�������]�w
	@Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return ModItems.Polymetal;
    }
	
	//�����ƶq�]�w: �ھھ��v����]���ŨM�w�����ƶq
	//�Y���]����>0  �̷ӵ����H���W�[�ƶq  �̤�2��  �̦h��(1+���]����)��
	@Override
	public int quantityDroppedWithBonus(int fortune, Random rand) {
		if (fortune > 0) {
			return 2 + rand.nextInt(fortune);
		}
		return 1;  //�L���] �h�����@��
	}
	
	
//	//�c�l����������~�ϥ�damageDropped �Ϩ䱼��metadata�������~  (quantityDropped�]��metadata����)
//	@Override
//	public int damageDropped(int metadata) {
//	    return this.meta;
//	}

}
