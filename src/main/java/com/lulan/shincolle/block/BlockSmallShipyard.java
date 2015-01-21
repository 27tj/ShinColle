package com.lulan.shincolle.block;

import java.util.Random;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.handler.GuiHandler;
import com.lulan.shincolle.reference.GUIs;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockSmallShipyard extends BasicBlockContainer {

	public Random rand = new Random();
	public static boolean keepInventory = false;
	private static final double[] smoke1 = new double[] {0.72, 1.1, 0.55};	//�D�ϧw �ɤl��m
	private static final double[] smoke2 = new double[] {0.22, 0.8, 0.7};	//���ϧw �ɤl��m
	private static final double[] smoke3 = new double[] {0.47, 0.6, 0.25};	//�p�ϧw �ɤl��m
	
	
	public BlockSmallShipyard() {
		super(); //�����w���A �w�]�Y��rock
		this.setBlockName("BlockSmallShipyard");
		this.setHardness(10F);
	    this.setHarvestLevel("pickaxe", 3);
	    this.setLightLevel(4);
	}
	
	//�D�зǤ�Τ��  �n��-1��ܥΦۤv��render
	@Override
	public int getRenderType() {
		return -1;	//-1 = non standard render
	}
	
	//�D�зǤ�Τ��  �]��false
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	//�D�зǤ�Τ��  �]��false
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int i) {
		return new TileEntitySmallShipyard();
	}
	
	//���������, �����䤺�e��
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if(!keepInventory) {
			TileEntitySmallShipyard tileentity = (TileEntitySmallShipyard)world.getTileEntity(x, y, z);
		
			//���tile entity��, ���y����slot���e��, �M�ᰵ��entity�����X��
			if(tileentity != null) {
				for(int i = 0; i < tileentity.getSizeInventory(); i++) {  //check all slots
					ItemStack itemstack = tileentity.getStackInSlot(i);
		
					if(itemstack != null) {
						float f = this.rand.nextFloat() * 0.8F + 0.1F;  //�]�w�n�H���Q�X��range
						float f1 = this.rand.nextFloat() * 0.8F + 0.1F;
						float f2 = this.rand.nextFloat() * 0.8F + 0.1F;
		
						while(itemstack.stackSize > 0) {
							int j = this.rand.nextInt(21) + 10;
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
		
						world.spawnEntityInWorld(item);	//�ͦ�entity
						}
					}
				}	
				world.func_147453_f(x, y, z, block);	//???
			}
		}
		//�I�s�����breakBlock, �|��tile entity������
		super.breakBlock(world, x, y, z, block, meta);
	}
	
	/**�k���I�����ɩI�s����k
	 * �Ѽ�: world,���x,y,z,���a,���a���V,���a�I�쪺x,y,z
	 */	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {	//client�ݥu�ݭn����true
    		return true;
    	}
		else if (!player.isSneaking()) {  //server�ݻݭn�B�z���a�ʧ@, �pshift��
    		TileEntitySmallShipyard entity = (TileEntitySmallShipyard) world.getTileEntity(x, y, z);
    		
    		if (entity != null) {	//�}�Ҥ��GUI �Ѽ�:���a,mod instance,gui ID,world,�y��xyz
    			FMLNetworkHandler.openGui(player, ShinColle.instance, GUIs.SMALLSHIPYARD, world, x, y, z);
    		}
    		return true;
    	}
    	else {
    		return false;
    	}
    }
	
	//spawn particle: largesmoke, posX, posY, posZ, motionX, motionY, motionZ
	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
		int meta = world.getBlockMetadata(x, y, z);
		
		//���o����᪺�s��m
		double[] smokeR1 = new double[3];
		double[] smokeR2 = new double[3];
		double[] smokeR3 = new double[3];
		
		smokeR1 = ParticleHelper.getNewPosition(smoke1[0], smoke1[1], smoke1[2], meta, 1);
		smokeR2 = ParticleHelper.getNewPosition(smoke2[0], smoke2[1], smoke2[2], meta, 1);
		smokeR3 = ParticleHelper.getNewPosition(smoke3[0], smoke3[1], smoke3[2], meta, 1);
		
		//if active -> spawn smoke
		if(meta>3) {	//meta=4~7 = active
			//world.setBlockMetadataWithNotify(x, y, z, 0, 2);	//send meta update packet
			switch(rand.nextInt(3)) {	//�ϤT�ڷϧw���}�_��
			case 0:
				//�D�ϧw�S��
				world.spawnParticle("smoke", (double)x+smokeR1[0], (double)y+smokeR1[1], (double)z+smokeR1[2], 0.0D, 0D, 0.0D);
				world.spawnParticle("smoke", (double)x+smokeR1[0], (double)y+smokeR1[1]+0.1D, (double)z+smokeR1[2], 0.0D, 0.005D, 0.0D);
				world.spawnParticle("smoke", (double)x+smokeR1[0], (double)y+smokeR1[1]+0.2D, (double)z+smokeR1[2], 0.0D, 0.01D, 0.0D);
				//�p�ϧw�S��
				world.spawnParticle("smoke", (double)x+smokeR3[0], (double)y+smokeR3[1], (double)z+smokeR3[2], 0.0D, 0D, 0.0D);
				world.spawnParticle("smoke", (double)x+smokeR3[0], (double)y+smokeR3[1]+0.1D, (double)z+smokeR3[2], 0.0D, 0.01D, 0.0D);
				break;
			case 1:
				//�D�ϧw�S��
				world.spawnParticle("smoke", (double)x+smokeR1[0], (double)y+smokeR1[1], (double)z+smokeR1[2], 0.0D, 0D, 0.0D);
				world.spawnParticle("smoke", (double)x+smokeR1[0], (double)y+smokeR1[1]+0.1D, (double)z+smokeR1[2], 0.0D, 0.005D, 0.0D);
				world.spawnParticle("smoke", (double)x+smokeR1[0], (double)y+smokeR1[1]+0.2D, (double)z+smokeR1[2], 0.0D, 0.01D, 0.0D);				//���ϧw�S��
				//���ϧw�S��
				world.spawnParticle("smoke", (double)x+smokeR2[0], (double)y+smokeR2[1], (double)z+smokeR2[2], 0.0D, 0D, 0.0D);
				world.spawnParticle("smoke", (double)x+smokeR2[0], (double)y+smokeR2[1]+0.1D, (double)z+smokeR2[2], 0.0D, 0.01D, 0.0D);
				break;
			case 2:
				//���ϧw�S��
				world.spawnParticle("smoke", (double)x+smokeR2[0], (double)y+smokeR2[1], (double)z+smokeR2[2], 0.0D, 0D, 0.0D);
				world.spawnParticle("smoke", (double)x+smokeR2[0], (double)y+smokeR2[1]+0.1D, (double)z+smokeR2[2], 0.0D, 0.01D, 0.0D);
				//�p�ϧw�S��
				world.spawnParticle("smoke", (double)x+smokeR3[0], (double)y+smokeR3[1], (double)z+smokeR3[2], 0.0D, 0D, 0.0D);
				world.spawnParticle("smoke", (double)x+smokeR3[0], (double)y+smokeR3[1]+0.1D, (double)z+smokeR3[2], 0.0D, 0.01D, 0.0D);
				break;
			default:
				break;
			}//end switch		
		}//end if
	}

	//�Ntile entity��Ƽg��block metadata��
	public static void updateBlockState(boolean isBuilding, World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		TileEntity entity = world.getTileEntity(x, y, z);

		if (isBuilding) {	//�Ұʤ�, �Nmeta�]��4~7
			if(meta < 4) {	//�ˬd�@�Umeta < 4 �~�ݭn��smeta
				world.setBlockMetadataWithNotify(x, y, z, meta+4, 2);
			}
		}
		else {				//�Ұʤ�, �Nmeta�]��0~3
			if(meta > 3) {	//�ˬd�@�Umeta > 3 �~�ݭn��smeta
				world.setBlockMetadataWithNotify(x, y, z, meta-4, 2);
			}
		}
		
		//unknow function
		if (entity != null) {
			entity.validate();
			world.setTileEntity(x, y, z, entity);
		}
		
	}

	

}
