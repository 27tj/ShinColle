package com.lulan.shincolle.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.BasicTileMulti;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.MulitBlockHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockGrudgeHeavy extends BasicBlockMulti {
	public BlockGrudgeHeavy() {
		super(Material.sand);
		this.setBlockName("BlockGrudgeHeavy");
		this.setHarvestLevel("shovel", 0);
	    this.setHardness(3F);
	    this.setLightLevel(1F);
	    this.setStepSound(soundTypeSand);		
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileMultiGrudgeHeavy();
	}
		
	//�T��Ӥ�����ͱ�����, �Ҧ�����������bbreakBlock�ͦ�
	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
		return ret;	//�����^�ǪŪ�array (�����null�|�Q�XNPE)
    }
	
	//�����m��, �N���~��mats�ƶq���X�s��tile��nbt��
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack itemstack) {
		TileEntity tile = world.getTileEntity(x, y, z);
		
		if(tile != null && tile instanceof TileMultiGrudgeHeavy) {
			//�Nmats��Ʀs��matStock��
			if(itemstack.hasTagCompound()) {
				NBTTagCompound nbt = itemstack.getTagCompound();
				int[] mats = nbt.getIntArray("mats");
		        
				((TileMultiGrudgeHeavy)tile).setMatStock(0, mats[0]);
				((TileMultiGrudgeHeavy)tile).setMatStock(1, mats[1]);
				((TileMultiGrudgeHeavy)tile).setMatStock(2, mats[2]);
				((TileMultiGrudgeHeavy)tile).setMatStock(3, mats[3]);
			}
		}
	}
	
	//���������, �����䤺�e��
	//heavy grudge������, �|��matBuild��matStock�s�bitem��nbt��
	//�`�Ntile�|�b�o�������, �ҥHgetDrops�I�s�ɤw�g�줣��tile, ����tile��ƭn�d�U�����n�b����k����
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		TileEntity getTile = world.getTileEntity(x, y, z);
		TileMultiGrudgeHeavy tile = null;
		
		//�T�{��쪺�Ogrudge heavy, �H���N�~
		if(getTile instanceof TileMultiGrudgeHeavy) {
			tile = (TileMultiGrudgeHeavy)getTile;
		}
		
		if(tile != null) {
			//���y����slot���e��, �M�ᰵ��entity�����X��
			for(int i = 0; i < tile.getSizeInventory(); i++) {
				ItemStack itemstack = tile.getStackInSlot(i);

				if(itemstack != null) {
					//�]�w�n�H���Q�X��range
					float f = world.rand.nextFloat() * 0.8F + 0.1F;
					float f1 = world.rand.nextFloat() * 0.8F + 0.1F;
					float f2 = world.rand.nextFloat() * 0.8F + 0.1F;

					while(itemstack.stackSize > 0) {
						int j = world.rand.nextInt(21) + 10;
						//�p�G���~�W�L�@���H���ƶq, �|����h�|�Q�X
						if(j > itemstack.stackSize) {  
							j = itemstack.stackSize;
						}

						itemstack.stackSize -= j;
						//�Nitem����entity, �ͦ���@�ɤW
						EntityItem item = new EntityItem(world, (double)((float)x + f), (double)((float)y + f1), (double)((float)z + f2), new ItemStack(itemstack.getItem(), j, itemstack.getItemDamage()));
						//�p�G��NBT tag, �]�n�ƻs�쪫�~�W
						if(itemstack.hasTagCompound()) {
							item.getEntityItem().setTagCompound((NBTTagCompound)itemstack.getTagCompound().copy());
						}
					world.spawnEntityInWorld(item);	//�ͦ�item entity
					}
				}
			}
			
			//���ymatBuild��matStock�O�_���s��, ��������s��block item�W�åͦ���world��
			EntityItem item = new EntityItem(world, (double)x, (double)y+0.5D, (double)z, new ItemStack(ModBlocks.BlockGrudgeHeavy, 1 ,0));
			NBTTagCompound nbt = new NBTTagCompound();
			
			int[] mats = new int[4];
			mats[0] = tile.getMatBuild(0) + tile.getMatStock(0);
			mats[1] = tile.getMatBuild(1) + tile.getMatStock(1);
			mats[2] = tile.getMatBuild(2) + tile.getMatStock(2);
			mats[3] = tile.getMatBuild(3) + tile.getMatStock(3);		
			
			//�u�n���@�ӧ��Ƥ���0�N�snbt, �Y�Ҭ�0�N���ݭn�s
			nbt.setIntArray("mats", mats);
			if(mats[0] != 0 || mats[1] != 0 || mats[2] != 0 || mats[3] != 0) {
				item.getEntityItem().setTagCompound(nbt);	//�Nnbt�s��entity item��
			}
			world.spawnEntityInWorld(item);				//�ͦ�item entity
			
			world.func_147453_f(x, y, z, block);		//alert block changed
		}
		//�I�s�����breakBlock, �|��tile entity������
		super.breakBlock(world, x, y, z, block, meta);
	}
	
	//�H���o�X�ǰe������
	@SideOnly(Side.CLIENT)
    public void randomDisplayTick(World p_149734_1_, int p_149734_2_, int p_149734_3_, int p_149734_4_, Random p_149734_5_) {
		//play portal sound
		if (p_149734_5_.nextInt(50) == 0) {
            p_149734_1_.playSound((double)p_149734_2_ + 0.5D, (double)p_149734_3_ + 0.5D, (double)p_149734_4_ + 0.5D, "portal.portal", 0.5F, p_149734_5_.nextFloat() * 0.4F + 0.8F, false);
        }
    }


}
