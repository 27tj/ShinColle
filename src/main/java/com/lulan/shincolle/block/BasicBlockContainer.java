package com.lulan.shincolle.block;

import com.lulan.shincolle.creativetab.CreativeTabSC;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

abstract public class BasicBlockContainer extends BlockContainer {

	//���w�������block
	public BasicBlockContainer(Material material) {
		super(material);
		this.setCreativeTab(CreativeTabSC.SC_TAB);	//�[�J��creative tab��
	}
	
	//�L���w������ �w�]��rock��
	public BasicBlockContainer() {
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
	
	//����icon, ������custom render block
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
	}
	
	//new tile entity instance in child class 
	abstract public TileEntity createNewTileEntity(World world, int i);
	
	/**�����U�ɳ]�w��¦V
	 * parm: world,x�y��,y�y��,z�y��,���a,���~
	 * �]�wmeta��k: setBlockMetadataWithNotify parm:x,y,z,metadata,flag(1:�]�w������nupdate  2:���F1�ٵo�e��s�ʥ]��client)
	 * metadata�N����V�n�ۦ�M�w  �@��̷�block����V�K�Ϫ�����: 
	 * 0:bottom 1:top 2:north 3:south 4:west 5:east
	 */
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack itemstack) {		
		//�Ѫ��a�����ਤ�רM�w������¦V
		int facecase = MathHelper.floor_double((double)(entity.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		
		if (facecase == 0) {	//player face south , block -> 2:north
		    world.setBlockMetadataWithNotify(x, y, z, 0, 2);
		}     
		if (facecase == 1) {	//player face west  , block -> 5:east
			world.setBlockMetadataWithNotify(x, y, z, 1, 2);
		}      
		if (facecase == 2) {	//player face north , block -> 3:south
			world.setBlockMetadataWithNotify(x, y, z, 2, 2);      	
		}       
		if (facecase == 3) {	//player face east  , block -> 4:west
			world.setBlockMetadataWithNotify(x, y, z, 3, 2);
		}
   }


}
