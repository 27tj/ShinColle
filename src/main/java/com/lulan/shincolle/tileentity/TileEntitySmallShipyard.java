package com.lulan.shincolle.tileentity;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.FluidStack;

import com.lulan.shincolle.block.BlockSmallShipyard;
import com.lulan.shincolle.capability.CapaInventory;
import com.lulan.shincolle.crafting.SmallRecipes;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.TileEntityHelper;

/** Fuel Cost = BaseCost + CostPerMaterial * ( TotalMaterialAmount - minAmount * 4 )
 *  Total Build Time = FuelCost / buildSpeed
 *  MaxBuildTime / MaxFuelCost = 8min / 460800  (48 fuel per tick)
 *  MinBuildTime / MinFuelCost = 1min / 57600
 * 	MaxMaterial / MaxFuelCost = 64*4 / 460800
 *  MinMaterial / MinFuelCost = 16*4 / 57600 = BaseCost(57600) CostPerMaterial(2100)
 *  
 *  Slots:
 *    0: grudge
 *    1: abyssium
 *    2: ammo
 *    3: polymetal
 *    4: fuel
 *    5: output
 */
public class TileEntitySmallShipyard extends BasicTileInventory implements ITileLiquidFurnace, ITickable
{
//	//fluid tank  (TODO IFluidHandler capability)
//	private static final int TANKCAPA = FluidContainerRegistry.BUCKET_VOLUME;
//	private static final Fluid F_LAVA = FluidRegistry.LAVA;
//	private FluidTank tank = new FluidTank(new FluidStack(F_LAVA, 0), TANKCAPA);
	
	//furnace
	private int powerConsumed = 0;	//已花費的能量
	private int powerRemained = 0;	//剩餘燃料
	private int powerGoal = 0;		//需要達成的目標能量
	private int buildType = 0;		//type 0:none 1:ship 2:equip 3:ship loop 4: equip loop
	private int[] buildRecord;
	private boolean isActive;		//是否正在建造中, 此為紀錄isBuilding是否有變化用
	public static int POWERINST;	//power per instant material
	public static int BUILDSPEED;	//power cost per tick
	public static int POWERMAX; 	//max power storage
	public static float FUELMAGN; 	//fuel magnification
	private static final int[] ALLSLOTS = new int[] {0, 1, 2, 3, 4, 5};  //dont care side

	
	/** 注意constructor只會在server端呼叫, client端需要另外init以免噴出NPE */
	public TileEntitySmallShipyard()
	{
		super();
		
		//slots: 0:grudge 1:abyss 2:ammo 3:poly 4:fuel 5:output
		this.itemHandler = new CapaInventory(6, this);
		this.isActive = false;
		this.syncTime = 0;
		
		POWERMAX = (int) ConfigHandler.shipyardSmall[0];
		BUILDSPEED = (int) ConfigHandler.shipyardSmall[1];
		FUELMAGN = (float) ConfigHandler.shipyardSmall[2];
		POWERINST = BUILDSPEED * 2400;
		
	}
	
	@Override
	public String getRegName()
	{
		return BlockSmallShipyard.TILENAME;
	}
	
	@Override
	public int getGuiIntID()
	{
		return ID.Gui.SMALLSHIPYARD;
	}

	//依照輸出入口設定, 決定漏斗等裝置如何輸出入物品到特定slot中
	//注意: 此設定必須跟getCapability相同以免出現bug
	@Override
	public int[] getSlotsForFace(EnumFacing side)
	{
		return ALLSLOTS;
	}
	
	//load data
	@Override
    public void readFromNBT(NBTTagCompound compound)
	{
        super.readFromNBT(compound);	//從nbt讀取方塊的xyz座標
        
        //load vars
        powerConsumed = compound.getInteger("consumedPower");
        powerRemained = compound.getInteger("remainedPower");
        powerGoal = compound.getInteger("goalPower");
        buildType = compound.getInteger("buildType");
        setBuildRecord(compound.getIntArray("buildRecord"));
    }
	
	//save data
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound)
	{
		super.writeToNBT(compound);

		//save vars
		compound.setInteger("consumedPower", powerConsumed);
		compound.setInteger("remainedPower", powerRemained);
		compound.setInteger("goalPower", powerGoal);
		compound.setInteger("buildType", buildType);
		compound.setIntArray("buildRecord", getBuildRecord());
		
		return compound;
	}
	
	//判定物品是否能放入該格子, 用於canExtractItem等方法
	//格子用途:0:grudge 1:abyss 2:ammo 3:poly 4:fuel 5:output
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemstack)
	{
		if (itemstack != null)
		{
			Item item = itemstack.getItem();
			int meta = itemstack.getItemDamage();
		
			switch(slot)
			{
			case 0:		//grudge slot
				return item == ModItems.Grudge;
			case 1:		//abyssium slot
				return item == ModItems.AbyssMetal && meta == 0;
			case 2:		//ammo slot
				return item == ModItems.Ammo && meta == 0;
			case 3:		//polymetal slot
				return item == ModItems.AbyssMetal && meta == 1;
			case 4:		//fuel slot
				return TileEntityHelper.getItemFuelValue(itemstack) > 0 || item == ModItems.InstantConMat;
			default:
				return false;
			}
		}
		else
		{
			return false;
		}
	}

	//使用管線/漏斗輸出時呼叫, 不適用於手動置入
	@Override
	public boolean canExtractItem(int slot, ItemStack item, EnumFacing face)
	{
		//只有output slot跟fuel slot的空bucket可以輸出, face ignore
		return (slot == 5) || (slot == 0 && item.getItem() == Items.BUCKET);
	}
	
	//建造ship方法
	public void buildComplete()
	{
		//若為無限loop建造, 則檢查record的紀錄
		if (this.buildType == ID.Build.EQUIP_LOOP || this.buildType == ID.Build.SHIP_LOOP)
		{
			for (int i = 0; i < 4; i++)
			{
				ItemStack item = itemHandler.getStackInSlot(i);
				
				//檢查材料是否足夠
				if (item == null || item.stackSize < getBuildRecord(i))
				{
					return;
				}
				//吃掉材料
				else
				{
					item.stackSize -= getBuildRecord(i);
					//材料用光, 清空該slot
					if (item.stackSize <= 0) itemHandler.setStackInSlot(i, null);
				}
			}

			//輸入材料數量, 取得build output到slot 5
			switch (this.buildType)
			{
			default:
			case ID.Build.SHIP_LOOP:
				itemHandler.setStackInSlot(5, SmallRecipes.getBuildResultShip(getBuildRecord()));
				break;
			case ID.Build.EQUIP_LOOP:
				itemHandler.setStackInSlot(5, SmallRecipes.getBuildResultEquip(getBuildRecord()));
				break;
			}
		}
		//單次或初次建造, 直接吃掉全部材料
		else
		{
			int[] matAmount = new int[4];
			//取得四樣材料數量
			matAmount = SmallRecipes.getMaterialAmount(itemHandler.getStacksInSlots(0, 4));

			//將輸入材料全部吃掉
			itemHandler.setStackInSlot(0, null);
			itemHandler.setStackInSlot(1, null);
			itemHandler.setStackInSlot(2, null);
			itemHandler.setStackInSlot(3, null);
			
			//輸入材料數量, 取得build output到slot 5
			switch (this.buildType)
			{
			default:
			case ID.Build.SHIP:			//build ship
			case ID.Build.SHIP_LOOP:
				itemHandler.setStackInSlot(5, SmallRecipes.getBuildResultShip(matAmount));
				break;
			case ID.Build.EQUIP:		//build equip
			case ID.Build.EQUIP_LOOP:
				itemHandler.setStackInSlot(5, SmallRecipes.getBuildResultEquip(matAmount));
				break;
			}
		}

	}
	
	//判定是否建造中
	public boolean isBuilding()
	{
		return hasRemainedPower() && canBuild();
	}
	
	//判定是否有燃料
	public boolean hasRemainedPower()
	{
		return powerRemained > BUILDSPEED;
	}
	
	//判定是否能建造
	public boolean canBuild()
	{
		if (powerGoal <= 0) return false;
		
		//若為無限loop建造, 則檢查record的紀錄
		if (this.buildType == ID.Build.EQUIP_LOOP || this.buildType == ID.Build.SHIP_LOOP)
		{		
			//檢查紀錄是否可以建造
			if (SmallRecipes.canRecipeBuild(getBuildRecord()))
			{
				//檢查材料是否足夠
				for (int i = 0; i < 4; i++)
				{
					ItemStack item = itemHandler.getStackInSlot(i);
					
					if (item == null || item.stackSize < getBuildRecord(i))
					{
						return false;
					}
				}
				
				return itemHandler.getStackInSlot(5) == null;
			}
		}
		else
		{
			return itemHandler.getStackInSlot(5) == null;
		}
		
		return false;
	}
	
	//取得建造花費
	public void calcPowerGoal()
	{
		//若為無限loop建造, 則計算record的紀錄
		if (this.buildType == ID.Build.EQUIP_LOOP || this.buildType == ID.Build.SHIP_LOOP)
		{
			//檢查紀錄是否可以建造
			if (SmallRecipes.canRecipeBuild(getBuildRecord()))
			{
				powerGoal = SmallRecipes.calcGoalPower(getBuildRecord());
			}
			else
			{
				powerGoal = 0;
			}
		}
		else
		{
			int[] itemAmount = new int[4];
			
			//計算材料量
			itemAmount = SmallRecipes.getMaterialAmount(itemHandler.getStacksInSlots(0, 4));
			
			//依照材料量計算goalPower, 若材料沒達minAmount則goalPower會得到0
			powerGoal = SmallRecipes.calcGoalPower(itemAmount);
		}
	}
	
	//方塊的流程進行方法
	//資料必須以markDirty標記block更新, 以及讀寫NBT tag來保存
	@Override
	public void update()
	{
		boolean sendUpdate = false;	//標紀要block update, 有要更新metadata時設為true

//		//null check TODO
//		if (this.buildRecord == null || this.buildRecord.length < 4)
//		{
//			this.buildRecord = new int[] {0, 0, 0, 0};
//		}

		//server side
		if (!worldObj.isRemote)
		{
			this.syncTime++;
			
			//update goalPower
			if (this.buildType != ID.Build.NONE)
			{
				this.calcPowerGoal();
			}
			else
			{
				this.powerGoal = 0;
			}
			
			//add item fuel
			if (TileEntityHelper.decrItemFuel(this))
			{
				sendUpdate = true;
			}
			
			//add liquid fuel
//			TileEntityHelper.decrLiquidFuel(this); //TODO update liquid handler
			
			//判定是否建造中, 每tick進行進度值更新, 若非建造中則重置進度值
			if (this.isBuilding())
			{
				ItemStack item = itemHandler.getStackInSlot(4);
				
				//在燃料格使用快速建造材料
				if (item != null && item.getItem() == ModItems.InstantConMat)
				{
					item.stackSize--;
					this.powerConsumed += POWERINST;
					
					if (item.stackSize == 0)
					{
						itemHandler.setStackInSlot(4, null);
					}
					
					sendUpdate = true;
				}
				
				this.powerRemained -= BUILDSPEED;	//fuel bar --
				this.powerConsumed += BUILDSPEED;	//build bar ++
				
				//power達標, 建造完成
				if (this.powerConsumed >= this.powerGoal)
				{
					this.buildComplete();	//建造出成品放到output slot
					this.powerConsumed = 0;
					this.powerGoal = 0;
					
					//continue build if mode = loop mode
					switch (buildType)
					{
					default:
					case ID.Build.SHIP:
					case ID.Build.EQUIP:		//reset build type
						this.buildType = ID.Build.NONE;
						break;
					case ID.Build.SHIP_LOOP:	//remain build type
					case ID.Build.EQUIP_LOOP:
						break;
					}
					
					sendUpdate = true;
				}
			}
			
			//非建造中, 重置build bar
			if (!this.canBuild())
			{
				this.powerConsumed = 0;
			}
			
			//若狀態有改變過, 則發送更新  ex:本來active 而燃料用光導致無法active時
			if (isActive != this.isBuilding())
			{
				isActive = this.isBuilding();
				sendUpdate = true;
			}
			
			//標紀要更新
			if (sendUpdate)
			{
				this.syncTime = 0;
				//update blockstate & send packet
				BlockSmallShipyard.updateBlockState(this.isBuilding(), this.worldObj, this.pos);
				//標記此方塊要更新, 以保證資料會存到硬碟
				this.markDirty();
			}
			
			//force update every 12000 ticks if no update
			if (this.syncTime > 12000)
			{
				this.syncTime = 0;
				
				//TODO force update
			}
		}//end server side

	}

	//計算fuel存量條
	public int getPowerRemainingScaled(int i)
	{
		return (powerRemained * i) / POWERMAX;
	}
	
	//計算建造時間 (換算成真實時間)
	public String getBuildTimeString()
	{
		//剩餘秒數 = (目標能量 - 目前能量) / (每tick增加能量) / 20
		int timeSec = (int) ((powerGoal - powerConsumed) / BUILDSPEED * 0.05F);  //get time (單位: sec)		
		return CalcHelper.getTimeFormated(timeSec);
	}
	
	//getter
	@Override
	public int getPowerConsumed()
	{
		return this.powerConsumed;
	}
	
	@Override
	public int getPowerRemained()
	{
		return this.powerRemained;
	}
	
	@Override
	public int getPowerGoal()
	{
		return this.powerGoal;
	}
	
	public int getBuildType()
	{
		return this.buildType;
	}
	
	public int[] getBuildRecord()
	{
		if (this.buildRecord == null || this.buildRecord.length < 4) this.buildRecord = new int[] {0,0,0,0};
		
		return this.buildRecord;
	}
	
	public int getBuildRecord(int id)
	{
		if (this.buildRecord == null || this.buildRecord.length < 4) this.buildRecord = new int[] {0,0,0,0};
		
		return this.buildRecord[id];
	}
	
	//setter
	@Override
	public void setPowerConsumed(int par1)
	{
		this.powerConsumed = par1;
	}
	
	@Override
	public void setPowerRemained(int par1)
	{
		this.powerRemained = par1;
	}
	
	@Override
	public void setPowerGoal(int par1)
	{
		this.powerGoal = par1;
	}
	
	public void setBuildType(int par1)
	{
		this.buildType = par1;
	}
	
	public void setBuildRecord(int[] par1)
	{
		if (par1 == null || par1.length < 4) par1 = new int[] {0,0,0,0};
		
		this.buildRecord = par1.clone();
	}
	
	public void setBuildRecord(int id, int par1)
	{
		if (this.buildRecord == null || this.buildRecord.length < 4) this.buildRecord = new int[] {0,0,0,0};
	
		this.buildRecord[id] = par1;
	}

	@Override
	public int getPowerMax()
	{
		return POWERMAX;
	}

	@Override
	public void setPowerMax(int par1) {}

//	@Override TODO update fluid handler
//	public int fill(ForgeDirection from, FluidStack fluid, boolean doFill)
//	{
//		if(TileEntityHelper.checkLiquidIsLava(fluid)) {
//			return tank.fill(fluid, doFill);
//		}
//		
//		return 0;
//	}
//
//	@Override
//	public FluidStack drain(ForgeDirection from, FluidStack fluid, boolean doDrain) {
//		return null;
//	}
//
//	@Override
//	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
//		return null;
//	}
//
//	@Override
//	public boolean canFill(ForgeDirection from, Fluid fluid) {
//		if(TileEntityHelper.checkLiquidIsLava(fluid)) {
//			return true;
//		}
//		
//		return false;
//	}
//
//	@Override
//	public boolean canDrain(ForgeDirection from, Fluid fluid) {
//		return false;
//	}
//
//	@Override
//	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
//		return new FluidTankInfo[] { tank.getInfo() };
//	}

	@Override
	public int getFluidFuelAmount()
	{
//		return this.tank.getFluidAmount(); TODO
		return 0;
	}

	@Override
	public FluidStack drainFluidFuel(int amount)
	{
//		return this.tank.drain(amount, true); TODO
		return null;
	}
	
	
}
