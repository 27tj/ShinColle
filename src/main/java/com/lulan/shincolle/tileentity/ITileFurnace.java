package com.lulan.shincolle.tileentity;

public interface ITileFurnace {

	/** �����B�z�w�g���ӱ����U�ƭ� */
	public int getPowerConsumed();
	public void setPowerConsumed(int par1);
	
	/** �����B�z�n�F�쪺�ؼпU�ƭ� */
	public int getPowerGoal();
	public void setPowerGoal(int par1);
	
	/** �ѤU���U�ƭ� */
	public int getPowerRemained();
	public void setPowerRemained(int par1);
	
	/** �U�ƭȤW�� */
	public int getPowerMax();
	public void setPowerMax(int par1);
	
}
