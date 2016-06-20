package dhu.btree;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//listKeys.size()==listValues.size()

public class LeafNode extends Node{
	
	private List<Integer> listKeys;		//存放查找键
	private List<Integer> listValues;	//存放查找键对应的记录地址
	
	private Integer nextNodePoint;		//指向下一个叶子节点的指针
	
	public LeafNode() {
		listKeys = new ArrayList<Integer>();
		listValues = new ArrayList<Integer>();
		nextNodePoint = null;
		setParentPoint(null);
	}
	
	//查找到key对应的值
	public int getValueOfKey(int key){		
		int index = listKeys.indexOf(key);  //返回key的下标，没有该key则返回-1
		if(index==-1){						//若没有要查找的键，则返回-1
			return index;
		}
		return listValues.get(index);   	//若有要查找的键，则返回该key对应的value
	}
	
	//查找该key的位置下标，没有该key则返回-1
	public int getKeyIndex(int key){
		return listKeys.indexOf(key);
	}
	
	//寻找待插入的位置index
	public int getInsertPosition(int newKey){	
		int index;
		for(index=0;index<listKeys.size();index++){   	
			if(newKey < listKeys.get(index)){
				break;
			}
		}
		return index;
	}
	
	//插入一个key
	public void insert(int newKey, int value){
		listKeys.add(getInsertPosition(newKey), newKey);
		listValues.add(getInsertPosition(newKey), value);
	}

	
	
	
	
	
	
	
	
	public List<Integer> getListKeys() {
		return listKeys;
	}

	public void setListKeys(List<Integer> listKeys) {
		this.listKeys = listKeys;
	}

	public List<Integer> getListValues() {
		return listValues;
	}

	public void setListValues(List<Integer> listValues) {
		this.listValues = listValues;
	}

	public Integer getNextNodePoint() {
		return nextNodePoint;
	}

	public void setNextNodePoint(Integer nextNodePoint) {
		this.nextNodePoint = nextNodePoint;
	}

	

}
