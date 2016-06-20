package dhu.btree;

public class Node {

	private int id;					//每个块有一个Id，表示该块的地址，这里模拟Id为树listTreeNode中该节点的下标
	private Integer parentPoint;	//每个块都有一个指向父节点的指针，若该指针为null，则表明该节点为父节点

	public Integer getParentPoint() {
		return parentPoint;
	}

	public void setParentPoint(Integer parentPoint) {
		this.parentPoint = parentPoint;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
}
