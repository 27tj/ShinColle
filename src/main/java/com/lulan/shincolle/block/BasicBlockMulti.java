package com.lulan.shincolle.block;

import java.util.ArrayList;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.creativetab.CreativeTabSC;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.reference.GUIs;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.BasicTileMulti;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.MulitBlockHelper;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

abstract public class BasicBlockMulti extends BasicBlockContainer {

	protected IIcon iconInvsible;
	
	
	//���w�������block
	public BasicBlockMulti(Material material) {
		super(material);
	}
	
	//�L���w������ �w�]��rock��
	public BasicBlockMulti() {
		super();
	}

	//multi block���|�̷�meta�����O�_��ܬ��z�����, �]���]false�ϳz�����A�ɥ��`render
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	//�ϥΨ��icon: �@��icon��z�����Aicon
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
		this.blockIcon = iconRegister.registerIcon(String.format("%s", getUnwrappedUnlocalizedName(this.getUnlocalizedName())));
		this.iconInvsible = iconRegister.registerIcon(Reference.MOD_ID+":BlockInvisible");
	}
	
	//meta�D0�ɪ�ܤw�g�Φ�multi block���c, �]���N����אּ�z���K��
	@Override
	public IIcon getIcon(int side, int meta) {
	    if(meta > 0) {
	    	return this.iconInvsible;
	    }
	    else {
	    	return this.blockIcon;
	    }    
	}
	
	//�����m�ɩI�s����k
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack itemstack) {
		
	}
	
	//�k���I�����ɩI�s����k
	//�Ѽ�: world,���x,y,z,���a,���a���V,���a�I�쪺x,y,z	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) {	//client�ݥu�ݭn����true
    		return true;
    	}
		else if(!player.isSneaking()) {	//server�ݰ����O�_�i�H����	
			BasicTileMulti entity = (BasicTileMulti)world.getTileEntity(x, y, z);

			if(entity != null) {
				if(entity.hasMaster()) {	//�Ӥ���w�g����, �h���}GUI
					LogHelper.info("DEBUG : open multi block GUI");
					switch(entity.getStructType()) {
					case GUIs.LARGESHIPYARD:
						FMLNetworkHandler.openGui(player, ShinColle.instance, GUIs.LARGESHIPYARD, world, 
								entity.getMasterX(), entity.getMasterY(), entity.getMasterZ());
						break;
					}
					return true;			//�w�}��GUI, �^��true�קK��W���F��ΥX�h
				}
				else {						//�Ӥ���|������, �ˬd�O�_�i�H����
					if(entity instanceof TileMultiGrudgeHeavy) {
						int type = MulitBlockHelper.checkMultiBlockForm(world, x, y, z);
						if(type > 0) {
							MulitBlockHelper.setupStructure(world, x, y, z, type);
							LogHelper.info("DEBUG : check multi block form: type "+type);
							return true;
						}				
					}		
				}
			}
		}

    	return false;	//�S�Ʊ���, �^��false�h�|�ܦ��ϥΤ�W�����~
    }
	
	//�Yblock�P�򦳤������, �h�����@�����c�O�_����
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
//		BasicTileMulti tile = (BasicTileMulti) world.getTileEntity(x, y, z);
//	    
//	    if(tile != null && tile.hasMaster()) {	//�Y�w�g����, �h�ˬd�Ϊ��O�_���M����
//            if(MulitBlockHelper.checkMultiBlockForm(world, tile.getMasterX(), tile.getMasterY(), tile.getMasterZ()) <= 0) {
//            	MulitBlockHelper.resetStructure(world, tile.getMasterX(), tile.getMasterY(), tile.getMasterZ());
//            }
//        }
//	    super.onNeighborBlockChange(world, x, y, z, block);
	}
	
	//���������, �����䤺�e��
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		BasicTileMulti tile = (BasicTileMulti)world.getTileEntity(x, y, z);

		//�쥻�Φ����c, �h�Ѱ���
		if(!world.isRemote && tile != null && tile.hasMaster()) {
			MulitBlockHelper.resetStructure(world, tile.getMasterX(), tile.getMasterY(), tile.getMasterZ());
		}
		
//			//���tile entity��, ���y����slot���e��, �M�ᰵ��entity�����X��
//			if(tile != null) {
//				for(int i = 0; i < tile.getSizeInventory(); i++) {  //check all slots
//					ItemStack itemstack = tile.getStackInSlot(i);
//	
//					if(itemstack != null) {
//						float f = world.rand.nextFloat() * 0.8F + 0.1F;  //�]�w�n�H���Q�X��range
//						float f1 = world.rand.nextFloat() * 0.8F + 0.1F;
//						float f2 = world.rand.nextFloat() * 0.8F + 0.1F;
//	
//						while(itemstack.stackSize > 0) {
//							int j = world.rand.nextInt(21) + 10;
//							//�p�G���~�W�L�@���H���ƶq, �|����h�|�Q�X
//							if(j > itemstack.stackSize) {  
//								j = itemstack.stackSize;
//							}
//	
//							itemstack.stackSize -= j;
//							//�Nitem����entity, �ͦ���@�ɤW
//							EntityItem item = new EntityItem(world, (double)((float)x + f), (double)((float)y + f1), (double)((float)z + f2), new ItemStack(itemstack.getItem(), j, itemstack.getItemDamage()));
//							//�p�G��NBT tag, �]�n�ƻs�쪫�~�W
//							if(itemstack.hasTagCompound()) {
//								item.getEntityItem().setTagCompound((NBTTagCompound)itemstack.getTagCompound().copy());
//							}
//	
//						world.spawnEntityInWorld(item);	//�ͦ�entity
//						}
//					}
//				}	
//				world.func_147453_f(x, y, z, block);	//alert block changed
//			}

		//�I�s�����breakBlock, �|��tile entity������
		super.breakBlock(world, x, y, z, block, meta);
	}
	
	//�Ntile entity��Ƽg��block metadata��
	public static void updateBlockState(World world, int x, int y, int z, int type) {
		BasicTileMulti tile = (BasicTileMulti)world.getTileEntity(x, y, z);
		world.setBlockMetadataWithNotify(x, y, z, type, 2);
	}
	
	//���}master�����, �������O�dtile entity�����
//	@Override
//	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
//        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
//
//        int count = quantityDropped(metadata, fortune, world.rand);
//        for(int i = 0; i < count; i++)
//        {
//            Item item = getItemDropped(metadata, world.rand, fortune);
//            if (item != null)
//            {
//                ret.add(new ItemStack(item, 1, damageDropped(metadata)));
//            }
//        }
//        return ret;
//    }

}
