package dhu.btree;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class BTree {

	private int n;					// 参数n,表明每个节点最多存放n个查找键，n+1个指针
	private int min_leafNode_keyNumber;    		// = (n+1)/2;		//叶节点最少(n+1)/2向下取整 个查找键
	private int min_interiorNode_pointNumber;  	// = ( (n+1)%2 == 0 ? (n+1)/2 : (n+1)/2+1 ); //非叶节点最少(n+1)/2向上取整 个指针
	
	private int rootId;			//如果只有一个块，则root为叶子节点，否则为内部节点
	private List<Node> listTreeNode;
	
	private List<Integer> listEmptyBlockId;		//保存已删除的块的Id
	
	public BTree(int n) {
		this.n=n;
		this.min_leafNode_keyNumber = (n+1)/2;
		this.min_interiorNode_pointNumber =( (n+1)%2 == 0 ? (n+1)/2 : (n+1)/2+1 );
		listTreeNode = new ArrayList<Node>();
		LeafNode firstNode = new LeafNode();
		firstNode.setId(0);
		listTreeNode.add(firstNode);	//初始化BTree只有一个空节点
		rootId=0;
		
		listEmptyBlockId = new ArrayList<Integer>();
	}
	
	
	//查找key对应的value，没有相应的key则返回-1
	public int searchKey(int rootId, int key){				//这里考虑了树的高度>=1,和0两种情况。//如果只有一个块，则root为叶子节点，否则为根节点
		Node root = listTreeNode.get(rootId);		//读取每一个块
		if(root instanceof LeafNode){						//如果是叶子节点的话就直接查找key
			int res = ((LeafNode) root).getValueOfKey(key); //res要么是-1，要么是value
			if(res == -1){
				System.out.println("The key "+key+" is not exists.");
			}
			return res;
		}else{												//如果是非叶子节点的话，递归查找叶子节点
			int childId = ((InteriorNode) root).getChildNodeId(key);
			return searchKey(childId, key);
		}
	}
	
	//范围查找，返回[fromKey, toKey]对应的[fromValue, toValue]
	public List<Integer> searchRange(int fromKey, int toKey){
		List<Integer> listValue = new ArrayList<Integer>();
		LeafNode fromNode = findAimNode(rootId, fromKey);
		LeafNode toNode = findAimNode(rootId, toKey);
		int count = 0;	//叶子节点个数 
		
		//从fromNode到toNode之前Node的所有value加入到listValue
		LeafNode tempNode = fromNode;
		while(tempNode != toNode){
			for(int value: tempNode.getListValues()){
				listValue.add(value);
			}
			tempNode = (LeafNode) listTreeNode.get(tempNode.getNextNodePoint());
			count++;
		}
		
		count++;
		
		//将toNode的0~toKey对应的value加入listValue
		int index = toNode.getListKeys().indexOf(toKey);		//**********在此假定数据库键值不重复
		if(index == -1){	//如果toKey不存在树中
			index = toNode.getInsertPosition(toKey)-1;
		}
		for(int i=0; i<=index;i++){	
			listValue.add(toNode.getListValues().get(i));
		}
		
		//将fromNode的fromValue之前的value从listValue去除
		index = fromNode.getListKeys().indexOf(fromKey);
		if(index ==-1){
			index = fromNode.getInsertPosition(fromKey);
		}
		for(int i = 0; i<index ;i++){
			listValue.remove(0);
		}
		
		System.out.println("   IO number: "+(getBTreeHeight()-1+count+listValue.size()));
		
		return listValue;
	}
	
	//往BTree中插入新键时，找到待插入键的目标块，即叶子节点。
	//此方法也可以作为删除时查找到要删除的key所在的叶子节点
	public LeafNode findAimNode(int rootId, int newKey){
		Node root = listTreeNode.get(rootId);
		if(root instanceof LeafNode){
			return (LeafNode) root;
		}
		else{
			int childId = ((InteriorNode)root).getChildNodeId(newKey);
			return findAimNode(childId, newKey);
		}
	}
	
	//从BTree中删除一个键，与insert互为逆过程。返回-1表明没有要删除的key，否则返回1删除成功
	public int delete(int key){
		LeafNode aimNode = findAimNode(rootId, key);
		int position = aimNode.getKeyIndex(key);	//要删除的key的下标索引
		if(position == -1){			
			System.out.println("The key "+key+" is not exists.");
			return -1;
		}
		
		int length = aimNode.getListKeys().size();
		if(length>min_leafNode_keyNumber || aimNode.getId()==rootId){		//如果目标节点中keyNumber大于     最小要求的min_leafNode_keyNumber。或者该Tree只有一个叶节点，则可以直接删除
			aimNode.getListKeys().remove(position);
			aimNode.getListValues().remove(position);
			if(aimNode.getId() == rootId && aimNode.getListKeys().size()==0){	//如果这棵树被删完了，那么从新开始rootId
				rootId = listTreeNode.size();
				listTreeNode.add(new LeafNode());
			}
			//TODO 把数据库中对应的记录删除？ 在外部做吧。 向数据库中插入删除操作在外部做
		}else{		//keyNumber<=min_leafNode_keyNumber，删除之后试图与前后兄弟节点合并
			
			aimNode.getListKeys().remove(position);
			aimNode.getListValues().remove(position);
			
			 InteriorNode parentNode = (InteriorNode) listTreeNode.get(aimNode.getParentPoint());
			 int preBrotherIndex = parentNode.getListPointers().indexOf(aimNode.getId())-1;					
			 int nextBrotherIndex = parentNode.getListPointers().indexOf(aimNode.getId())+1;
			 
			 LeafNode preBrotherNode = null;
			 LeafNode nextBrotherNode = null;
			 int preBrotherSize = 0; 	//= ((LeafNode)listTreeNode.get(preBrotherId)).getListKeys().size();
			 int nextBrotherSize = 0;	// = ((LeafNode)listTreeNode.get(nextBrotherId)).getListKeys().size();
			 
			 if(preBrotherIndex != -1){	//存在前兄弟节点
				 preBrotherNode = (LeafNode) listTreeNode.get(parentNode.getListPointers().get(preBrotherIndex));
				 preBrotherSize = preBrotherNode.getListKeys().size();
			 }
			 if(nextBrotherIndex != parentNode.getListPointers().size()){	//存在后兄弟节点
				 nextBrotherNode = (LeafNode) listTreeNode.get(parentNode.getListPointers().get(nextBrotherIndex));
				 nextBrotherSize = nextBrotherNode.getListKeys().size();
			 }
			 
			 if(preBrotherIndex != -1 && preBrotherSize > min_leafNode_keyNumber ){	//如果存在前兄弟节点，且它的键数大于最小的要求
				 //前兄弟节点最后一个key和value移到aimNode中的最前面，将父节点中原来指向aimNode的指针前面的那个key变为aimNode的第一个key
				 aimNode.getListKeys().add(0,preBrotherNode.getListKeys().get(preBrotherSize-1));
				 aimNode.getListValues().add(0, preBrotherNode.getListValues().get(preBrotherSize-1));
				 preBrotherNode.getListKeys().remove(preBrotherSize-1);
				 preBrotherNode.getListValues().remove(preBrotherSize-1);
				 int pointAimIndex = parentNode.getListPointers().indexOf(aimNode.getId()); 	//父节点中指向aimNode的指针索引
				 parentNode.getListKeys().set(pointAimIndex-1, aimNode.getListKeys().get(0));
			 }else if(nextBrotherIndex != parentNode.getListPointers().size() && nextBrotherSize > min_leafNode_keyNumber){	////如果存在后兄弟节点，且它的键数大于最小的要求
				 
				 //后兄弟节点第一个key和value移到aimNode中的最后面，将父节点中原来的指向nextBrotherNode的指针前面的那个key变为nextBrotherNode的第一个key
				 aimNode.getListKeys().add(nextBrotherNode.getListKeys().get(0));
				 aimNode.getListValues().add(nextBrotherNode.getListValues().get(0));
				 nextBrotherNode.getListKeys().remove(0);
				 nextBrotherNode.getListValues().remove(0);
				 int pointNextIndex = parentNode.getListPointers().indexOf(nextBrotherNode.getId());
				 parentNode.getListKeys().set(pointNextIndex-1, nextBrotherNode.getListKeys().get(0));
				 
				 //删掉的可能是aimNode的第一个节点，因此要修改父节点中原来的指向aimNode的指针相关的key
				 if(preBrotherIndex != -1){   //aimNode不是第一个节点，则要更新parentNode中指向aimNode指针之前的key
					 int pointAimIndex = parentNode.getListPointers().indexOf(aimNode.getId());
					 parentNode.getListKeys().set(pointAimIndex-1, aimNode.getListKeys().get(0));
				 }
				 
			 }else if(preBrotherIndex != -1){		//不能借，则合并           //如果存在前兄弟节点，则一定可以与之合并
				 //把aimNode合并到preBrotherNode中，删除aimNode,并调整parentNode
				 for(int aimKey: aimNode.getListKeys()){
					 preBrotherNode.getListKeys().add(aimKey);
				 }
				 for(int aimValue: aimNode.getListValues()){
					 preBrotherNode.getListValues().add(aimValue);
				 }
				 aimNode.getListKeys().clear();
				 aimNode.getListValues().clear();
				 
				 preBrotherNode.setNextNodePoint(aimNode.getNextNodePoint());
				 
				 //删除parentNode 指向aimNode的指针和前一个位置的key
				 int pointAimIndex = parentNode.getListPointers().indexOf(aimNode.getId());
				 parentNode.getListPointers().remove(pointAimIndex);
				 parentNode.getListKeys().remove(pointAimIndex-1);
				 
				 listEmptyBlockId.add(aimNode.getId());
				 aimNode = null;	//置为null表示删除，因为块空间无法删除
				 adjustTreeAfterDelete(parentNode);
				 
			 }else{			//否则，aimNode一定是parentNode的第一个子节点，且一定能向后合并
				 
				 //把aimNode合并到nextBrotherNode中，删除aimNode,并调整parentNode
				 int aimNodeKeyCursor = aimNode.getListKeys().size()-1;
				 while(aimNodeKeyCursor >= 0){
					 nextBrotherNode.getListKeys().add(0, aimNode.getListKeys().get(aimNodeKeyCursor));
					 nextBrotherNode.getListValues().add(0, aimNode.getListValues().get(aimNodeKeyCursor));
					 aimNodeKeyCursor--;
				 }
				 aimNode.getListKeys().clear();
				 aimNode.getListValues().clear();
				 
				 //删除parentNode 的第一个key和point
				 parentNode.getListKeys().remove(0);
				 parentNode.getListPointers().remove(0);
				 
				 //把parentNode的前兄弟节点的最后一个子节点的nextNodePoint指向nextBrotherNode
				 if(parentNode.getParentPoint() != null ){	//如果parentNode有父节点
					 int preParentIndex = ((InteriorNode)listTreeNode.get(parentNode.getParentPoint())).getListPointers().indexOf(parentNode.getId())-1;
					 if(preParentIndex != -1){				//而且有前兄弟节点
						 int preParentId = ((InteriorNode)listTreeNode.get(parentNode.getParentPoint())).getListPointers().get(preParentIndex);
						 int preParentPointSize = ((InteriorNode)listTreeNode.get(preParentId)).getListPointers().size();
						 int preParentLastChildId = ((InteriorNode)listTreeNode.get(preParentId)).getListPointers().get(preParentPointSize-1);
						((LeafNode)listTreeNode.get(preParentLastChildId)).setNextNodePoint(nextBrotherNode.getId());
						 
					 }
				 }
				 
				 listEmptyBlockId.add(aimNode.getId());
				 aimNode = null; 	
				 adjustTreeAfterDelete(parentNode);
			 }	
		}
		return 1;
		
	}
	
	//aimNode删除之后调用的此方法
	public void adjustTreeAfterDelete(InteriorNode aimNode){	//递归向上调整非叶节点
		if(aimNode.getParentPoint() == null){	//aimNode是根节点
			if(aimNode.getListPointers().size() == 1){	//根节点只剩一个指针，将其子节点设为新的根节点后，删除该根节点，
				Node newRootNode = listTreeNode.get(aimNode.getListPointers().get(0));
				newRootNode.setParentPoint(null);
				rootId = newRootNode.getId();
				aimNode.getListPointers().clear();
				listEmptyBlockId.add(aimNode.getId());
//////////////////////////////////////////////////////////////////TODO 这里只是把引用的值设为了null
				aimNode = null; 
			}else{
				//否则，什么也不做，树调整完毕
			}
			
			
		}else{
			int length = aimNode.getListPointers().size();
			if(length >= min_interiorNode_pointNumber){	//如果目标节点中的pointNumber删除一个后仍大于等于  最小要求
				//什么也不做
			}else{										//否则要调整该层及以上
				
				//尝试从兄弟节点中取来一个
				InteriorNode parentNode = (InteriorNode) listTreeNode.get(aimNode.getParentPoint());
				int preBrotherIndex = parentNode.getListPointers().indexOf(aimNode.getId())-1;					
				int nextBrotherIndex = parentNode.getListPointers().indexOf(aimNode.getId())+1;
				
				InteriorNode preBrotherNode = null;
				InteriorNode nextBrotherNode = null;
				int preBrotherSize = 0; 	//= ((InteriorNode)listTreeNode.get(preBrotherId)).getListPointers().size();
				int nextBrotherSize = 0;	// = ((InteriorNode)listTreeNode.get(nextBrotherId)).getListPointers().size();
				if(preBrotherIndex != -1){  //存在前兄弟节点
					preBrotherNode = (InteriorNode) listTreeNode.get(parentNode.getListPointers().get(preBrotherIndex));
					preBrotherSize = preBrotherNode.getListPointers().size();
				}
				if(nextBrotherIndex != parentNode.getListPointers().size()){	//存在后兄弟节点
					nextBrotherNode = (InteriorNode) listTreeNode.get(parentNode.getListPointers().get(nextBrotherIndex));
					nextBrotherSize = nextBrotherNode.getListPointers().size();
				}
				
				if(preBrotherIndex != -1 && preBrotherSize > min_interiorNode_pointNumber ){	//如果存在前兄弟节点，且它的point数大于最小的要求
					//将aimNode的子树的第一个叶子节点的第一个key添加到aimNode的最前面
					Node aimLeafNode = aimNode;
					while(aimLeafNode instanceof InteriorNode){
						aimLeafNode = listTreeNode.get( ((InteriorNode)aimLeafNode).getListPointers().get(0));
					}
					int aimLeafNodeKey = ((LeafNode)aimLeafNode).getListKeys().get(0);
					aimNode.getListKeys().add(0, aimLeafNodeKey);
					
					//将前兄弟节点最后一个point移到aimNode中的最前面，删除preBrotherNode的最后一个key
					aimNode.getListPointers().add(0, preBrotherNode.getListPointers().get(preBrotherSize-1));
					preBrotherNode.getListPointers().remove(preBrotherSize-1);
					preBrotherNode.getListKeys().remove(preBrotherSize-1-1);
					
					//aimNode调整后，修改aimNode新添加的第一个子节点的父节点为aimNode
					listTreeNode.get(aimNode.getListPointers().get(0)).setParentPoint(aimNode.getId());
					
					//将父节点中原来指向aimNode的指针前面的那个key变为aimNode子树的第一个叶子节点的第一个key
					aimLeafNode = aimNode;
					while(aimLeafNode instanceof InteriorNode){
						aimLeafNode = listTreeNode.get( ((InteriorNode)aimLeafNode).getListPointers().get(0));
					}
					aimLeafNodeKey = ((LeafNode)aimLeafNode).getListKeys().get(0);
					int aimIndex = parentNode.getListPointers().indexOf(aimNode.getId()); 	//parentNode中指向aimNode的指针的下标索引
					parentNode.getListKeys().set(aimIndex-1, aimLeafNodeKey);
					
				}else if(nextBrotherIndex != parentNode.getListPointers().size() && nextBrotherSize > min_interiorNode_pointNumber){	////如果存在后兄弟节点，且它的point数大于最小的要求
					//后兄弟节点第一个key和point复制到aimNode中的最后面
					aimNode.getListKeys().add(nextBrotherNode.getListKeys().get(0));
					aimNode.getListPointers().add(nextBrotherNode.getListPointers().get(0));
					
					//将aimNode的最后一个key替换为    nextBrotherNode子树的第一个叶子节点的第一个key
					Node nextBrotherLeafNode = nextBrotherNode;
					while(nextBrotherLeafNode instanceof InteriorNode){
						nextBrotherLeafNode = listTreeNode.get(((InteriorNode)nextBrotherLeafNode).getListPointers().get(0));
					}
					int nextBroLeafNodeKey = ((LeafNode)nextBrotherLeafNode).getListKeys().get(0);
					aimNode.getListKeys().set(aimNode.getListKeys().size()-1, nextBroLeafNodeKey);
					
					//删除nextBrotherNode的第一个key和point
					nextBrotherNode.getListKeys().remove(0);
					nextBrotherNode.getListPointers().remove(0);
					
					//aimNode调整后，修改aimNode新添加的最后一个子节点的父节点为aimNode
					listTreeNode.get(aimNode.getListPointers().get(aimNode.getListPointers().size()-1)).setParentPoint(aimNode.getId());
					
					//将父节点中原来指向nextBrotherNode的指针前面的那个key变为nextBrotherNode子树的第一个叶子节点的第一个key
					nextBrotherLeafNode = nextBrotherNode;
					while(nextBrotherLeafNode instanceof InteriorNode){
						nextBrotherLeafNode = listTreeNode.get(((InteriorNode)nextBrotherLeafNode).getListPointers().get(0));
					}
					nextBroLeafNodeKey = ((LeafNode)nextBrotherLeafNode).getListKeys().get(0);
					int pointToNextBroIndex = parentNode.getListPointers().indexOf(nextBrotherNode.getId());
					parentNode.getListKeys().set(pointToNextBroIndex-1, nextBroLeafNodeKey);
					
					
					if(preBrotherIndex != -1){ 	//如果aimNode不是第一个节点
						//将父节点中原来指向aimNode的指针前面的那个key变为aimNode子树的第一个叶子节点的第一个key
						Node aimLeafNode = aimNode;
						while(aimLeafNode instanceof InteriorNode){
							aimLeafNode = listTreeNode.get( ((InteriorNode)aimLeafNode).getListPointers().get(0));
						}
						int aimLeafNodeKey = ((LeafNode)aimLeafNode).getListKeys().get(0);
						int aimIndex = parentNode.getListPointers().indexOf(aimNode.getId()); 	//parentNode中指向aimNode的指针的下标索引
						parentNode.getListKeys().set(aimIndex-1, aimLeafNodeKey);
					}
					
				}else if(preBrotherIndex != -1){	//不能借，则合并           //如果存在前兄弟节点，则一定可以与之合并
					//将aimNode子树的第一个叶子节点的第一个key添加到preBrotherNode的最后面
					Node aimLeafNode = aimNode;
					while(aimLeafNode instanceof InteriorNode){
						aimLeafNode = listTreeNode.get( ((InteriorNode)aimLeafNode).getListPointers().get(0));
					}
					int aimLeafNodeKey = ((LeafNode)aimLeafNode).getListKeys().get(0);
					preBrotherNode.getListKeys().add(aimLeafNodeKey);
					
					//将aimNode中剩余的key和point移到preBrotherNode的最后面（从前往后移）。同时更新aimNode的子节点的parentPoint
					for(int tempKey: aimNode.getListKeys()){
						preBrotherNode.getListKeys().add(tempKey);
					}
					for(int tempPoint: aimNode.getListPointers()){
						preBrotherNode.getListPointers().add(tempPoint);
						listTreeNode.get(tempPoint).setParentPoint(preBrotherNode.getId());
					}
					aimNode.getListKeys().clear();
					aimNode.getListPointers().clear();
					
					//删除parentNode 指向aimNode的指针和前一个位置的key
					int aimIndex = parentNode.getListPointers().indexOf(aimNode.getId());
					parentNode.getListPointers().remove(aimIndex);
					parentNode.getListKeys().remove(parentNode.getListKeys().get(aimIndex-1));
					
					//删除aimNode节点
					listEmptyBlockId.add(aimNode.getId());
					aimNode = null;				//置为null表示删除，因为块空间无法删除
					
					//递归从parentNode向上调整树
					adjustTreeAfterDelete(parentNode);	
					
				}else{			//否则，aimNode一定是parentNode的第一个子节点，且一定能向后合并
					//将parentNode中指向nextBrotherNode的指针前面的那个key添加到nextBrotherNode的最前面
					//将nextBrotherNode子树的第一个叶子节点的第一个key添加到nextBrotherNode的最前面
					Node nextBrotherLeafNode = nextBrotherNode;
					while(nextBrotherLeafNode instanceof InteriorNode){
						nextBrotherLeafNode = listTreeNode.get(((InteriorNode)nextBrotherLeafNode).getListPointers().get(0));
					}
					int nextBroLeafNodeKey = ((LeafNode)nextBrotherLeafNode).getListKeys().get(0);
					nextBrotherNode.getListKeys().add(0, nextBroLeafNodeKey);
					
					//将aimNode中剩余的key和point移到nextBrotherNode的最前面，（从后往前移）。同时更新nextBrotherNode新进入的子节点的parentPoint为nextBrotherNode
					int aimNodeKeyCursor = aimNode.getListKeys().size()-1;
					nextBrotherNode.getListPointers().add(0, aimNode.getListPointers().get(aimNodeKeyCursor+1));
					listTreeNode.get(nextBrotherNode.getListPointers().get(0)).setParentPoint(nextBrotherNode.getId());
					while(aimNodeKeyCursor >= 0){
						nextBrotherNode.getListKeys().add(0, aimNode.getListKeys().get(aimNodeKeyCursor));
						nextBrotherNode.getListPointers().add(0, aimNode.getListPointers().get(aimNodeKeyCursor));
						listTreeNode.get(nextBrotherNode.getListPointers().get(0)).setParentPoint(nextBrotherNode.getId());
						aimNodeKeyCursor--;
					}
					aimNode.getListKeys().clear();
					aimNode.getListPointers().clear();
					
					//删除parentNode的第一个key和第一个point
					parentNode.getListKeys().remove(0);
					parentNode.getListPointers().remove(0);
					
					//删除aimNode节点
					listEmptyBlockId.add(aimNode.getId());
					aimNode = null;
					adjustTreeAfterDelete(parentNode);
					
				}
				
				
			}
			
		}
		
	}
	
	//向树中插入一个新的键-值对
	public void insert(int newKey, int value){
		LeafNode aimNode = findAimNode(rootId, newKey);
		int length = aimNode.getListKeys().size();
		int position = aimNode.getInsertPosition(newKey);	//待插入的位置
		if(length<n){		//叶子节点有空间
			aimNode.getListKeys().add(position, newKey);
			aimNode.getListValues().add(position, value);
		}else{				//叶子节点已满，会溢出，则新建一个叶子节点newNode
			LeafNode newNode = new LeafNode();
			newNode.setId(listTreeNode.size());
			newNode.setNextNodePoint(aimNode.getNextNodePoint());
			aimNode.setNextNodePoint(newNode.getId());
			
			//将aimNode最后面的MIN_LEAFNODE_KEY_NUMBER查找键和值移到newNode中
			if(position==aimNode.getListKeys().size()){
				newNode.getListKeys().add(0, newKey);
				newNode.getListValues().add(0, value);
			}else{
				newNode.getListKeys().add(0,aimNode.getListKeys().get(length-1));
				newNode.getListValues().add(0,aimNode.getListValues().get(length-1));
				aimNode.getListKeys().remove(length-1);
				aimNode.getListValues().remove(length-1);
				
				aimNode.getListKeys().add(position,newKey);
				aimNode.getListValues().add(position,value);
			}
			
			for(int i=0; i<min_leafNode_keyNumber-1;i++){
				int tempIndex =aimNode.getListKeys().size()-1;
				newNode.getListKeys().add(0,aimNode.getListKeys().get(tempIndex));
				newNode.getListValues().add(0, aimNode.getListValues().get(tempIndex));
				aimNode.getListKeys().remove(tempIndex);
				aimNode.getListValues().remove(tempIndex);
			}
			
			
			listTreeNode.add(newNode);
			adjustTreeAfterInsert(aimNode, newNode);
			
		}
		
	}
	
	//插入一个新键后需要调整BTree
	public void adjustTreeAfterInsert(Node aimNode, Node newNode){
		InteriorNode parent = null;				//parent一定是InteriorNode
		if(aimNode.getParentPoint()==null){		//aimNode是根节点，根节点分裂成了两个，则需要增加一个新根节点parent
			parent = new InteriorNode();
			parent.setId(listTreeNode.size());
			rootId = parent.getId();			//设置根节点为新建的节点
			parent.setParentPoint(null);
			parent.getListPointers().add(aimNode.getId());
			aimNode.setParentPoint(parent.getId());
			listTreeNode.add(parent);
		}else {									//否则parent不是根节点，取为aimNode的父节点
			parent = (InteriorNode) listTreeNode.get(aimNode.getParentPoint());
		}
		
		//为获取parent节点需要插入的键，需要找到新的节点对应的最底层的第一个叶子节点的第一个键。
		//新子树总是位于原子树的右边，新子树不是parent的第一颗子树,因此可以这样查找parent待插入的键
		Node newLeafNode = newNode;		
		while(newLeafNode instanceof InteriorNode){		//找到该新子树对应的第一个叶子节点
			newLeafNode = listTreeNode.get( ((InteriorNode)newLeafNode).getListPointers().get(0) );
		}
		int newKey = ((LeafNode)newLeafNode).getListKeys().get(0);  //新子树的第一个叶子节点的第一个键
		int position = parent.getInsertPosition(newKey);			//parent的中待插入新建的位置
		
		if(parent.getListKeys().size()<n){   	//父节点未满,插入新键和指针
			//在待插入的位置插入newKey，在该位置之后增加一个指针，指向新块（新子树）
			parent.getListKeys().add(position, newKey);
			parent.getListPointers().add(position+1, newNode.getId());//***********
			newNode.setParentPoint(parent.getId());
			return ;
			
		}else{    		//父节点已满，需要再增加一个指针，该父节点分裂为两个节点，重新向上调整Tree
			InteriorNode newParentNode = new InteriorNode();
			newParentNode.setId(listTreeNode.size());
			int length = parent.getListKeys().size();
			
			//假设parent插入newKey和point后，将parent中的min_interiorNode_pointNumber-1个key和min_interiorNode_pointNumber个point移到newParentNode
			if(position==length){	//若待插入的位置为该parent的最后，因为已满容不下，所以将该newKey,和point直接放到newParentNode
				newParentNode.getListKeys().add(0, newKey);
				newParentNode.getListPointers().add(0, newNode.getId());
				newNode.setParentPoint(newParentNode.getId());
			}else{					//否则， 将parent的最后一个key和point移到newParentNode,然后从parent中删除。这样就有空间将newKey和point插入parent
				newParentNode.getListKeys().add(0, parent.getListKeys().get(length-1));
				newParentNode.getListPointers().add(0, parent.getListPointers().get(length));
				listTreeNode.get(parent.getListPointers().get(length)).setParentPoint(newParentNode.getId());
				parent.getListKeys().remove(length-1);
				parent.getListPointers().remove(length);
				parent.getListKeys().add(position, newKey);
				parent.getListPointers().add(position+1, newNode.getId());
				newNode.setParentPoint(parent.getId());
			}
			for(int i=0;i<min_interiorNode_pointNumber-1-1;i++){	//newParentNode中已有一个key和point，故再从parent中移走min_interiorNode_pointNumber-1-1个key和point
				int tempIndex = parent.getListKeys().size()-1;
				newParentNode.getListKeys().add(0, parent.getListKeys().get(tempIndex));
				newParentNode.getListPointers().add(0, parent.getListPointers().get(tempIndex+1));
				listTreeNode.get(parent.getListPointers().get(tempIndex+1)).setParentPoint(newParentNode.getId());
				parent.getListKeys().remove(tempIndex);
				parent.getListPointers().remove(tempIndex+1);
			}
			//再从parent中移走一个point，并把parent中最后一个key删除
			int tempIndex = parent.getListKeys().size()-1;
			newParentNode.getListPointers().add(0, parent.getListPointers().get(tempIndex+1));
			listTreeNode.get(parent.getListPointers().get(tempIndex+1)).setParentPoint(newParentNode.getId());
			parent.getListPointers().remove(tempIndex+1);
			parent.getListKeys().remove(tempIndex); //将parent最后一个key删除
			
			
			listTreeNode.add(newParentNode);
			adjustTreeAfterInsert(parent, newParentNode);	//继续向上调整Tree
		}
		
	}
	
	
	
	
	
	
	
	//根据数据库来建BTree，实际上就是对树的插入操作
	public void buidTree(int db[]){
		for(int i=0;i<db.length;i++){
			insert(db[i], i);
			
		}
	}
	
	//根据数据库来建BTree，实际上就是对树的插入操作
	public void buidTree(List<Integer> dbList){
		
		//输出数据库中的所有数据，便于下面演示时参考
		for(int i=0; i< dbList.size();i++){
			if(dbList.get(i)!=null){
				System.out.print("["+i+"]"+dbList.get(i)+" ");
			}
		}
		System.out.println();
		
		
		for(int i=0;i<dbList.size();i++){
			insert(dbList.get(i), i);
			printTree();			//演示有元素插入时BTree的动态变化过程
		}
	}
	
	//按层次从高到低输出BTree,用队列实现广度优先遍历
	public void printTree(){
		System.out.print("\n------------- BTree in layer order is: ");
		System.out.print("    ( Parameter: "+n);
		System.out.print(" ,  root Id: "+rootId);
		System.out.print(" ,  nodes number: "+ getBTreeNodeNumber());
		System.out.println(" ,  height: "+getBTreeHeight()+" )");
		Node rootNode = listTreeNode.get(rootId);
		Queue<Node> queue = new LinkedList<Node>();
		queue.offer(rootNode);
		while(!queue.isEmpty()){
			Node node = queue.poll();
			printNodeInformation(node);
			if(node instanceof InteriorNode){
				for(int point: ((InteriorNode)node).getListPointers()){
					queue.offer(listTreeNode.get(point));
				}
			}
			
		}
		
	}
	
	//按照物理存储的顺序输出BTree
	public void printBTreePhysical(){
		System.out.print("\n------------- BTree in physical store order is: ");
		System.out.print("    ( Parameter: "+n);
		System.out.print(" ,  root Id: "+rootId);
		System.out.print(" ,  nodes number: "+ getBTreeNodeNumber());
		System.out.println(" ,  height: "+getBTreeHeight()+" )");
		for(Node node: listTreeNode){
			if(listEmptyBlockId.contains(node.getId())){
				continue;
			}
			printNodeInformation(node);
		}
			
			
		
	}
		
	//按顺序输出树的所有叶子节点
	public void printAllLeafNode(){
		System.out.println("\n-------------LeafNodes of BTree is:");
		Node leafNode = listTreeNode.get(rootId);
		while(leafNode instanceof InteriorNode){
			leafNode = listTreeNode.get(((InteriorNode)leafNode).getListPointers().get(0));
		}
		LeafNode node;
		for(node = (LeafNode) leafNode; node.getNextNodePoint()!=null; node=(LeafNode) listTreeNode.get(node.getNextNodePoint())){
			printNodeInformation(node);
		}
		printNodeInformation(node);
	}
	
	//输出一个节点的信息
	public void printNodeInformation(Node node){
		if ( node instanceof InteriorNode){
			System.out.print("InteNode id: "+node.getId()+"  ,  Keys: (");
			for(int i: ((InteriorNode) node).getListKeys()){
				System.out.print(i+" ");
			}
			System.out.print(")  ,  Pointers: (");
			for(int i : ((InteriorNode) node).getListPointers()){
				System.out.print(i+" ");
			}
			System.out.println(")  ,  PointNum: "+((InteriorNode) node).getListPointers().size());
		}else{
			System.out.print("LeafNode id: "+node.getId()+"  ,  Keys: (");
			for(int i: ((LeafNode) node).getListKeys()){
				System.out.print(i+" ");
			}
			System.out.print(")  ,  Values: (");
			for(int i: ((LeafNode) node).getListValues()){
				System.out.print(i+" ");
			}
			System.out.println(")  ,  NextId: "+((LeafNode) node).getNextNodePoint());
		}
	}
	
	//求树的高度
	public int getBTreeHeight(){
		int height = 0;
		Node leafNode = listTreeNode.get(rootId);
		while(leafNode instanceof InteriorNode){
			height++;
			leafNode = listTreeNode.get(((InteriorNode)leafNode).getListPointers().get(0));
		}
		return height+1;
	}
	
	//求树的节点数
	public int getBTreeNodeNumber(){
		return listTreeNode.size()-listEmptyBlockId.size();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public int getRootId() {
		return rootId;
	}

	public void setRootId(int rootId) {
		this.rootId = rootId;
	}

	public List<Node> getListTreeNode() {
		return listTreeNode;
	}

	public void setListTreeNode(List<Node> listTreeNode) {
		this.listTreeNode = listTreeNode;
	}

	public int getMIN_LEAFNODE_KEY_NUMBER() {
		return min_leafNode_keyNumber;
	}

	public void setMIN_LEAFNODE_KEY_NUMBER(int mIN_LEAFNODE_KEY_NUMBER) {
		min_leafNode_keyNumber = mIN_LEAFNODE_KEY_NUMBER;
	}

	public int getMIN_INTERIORNODE_POINT_NUMBER() {
		return min_interiorNode_pointNumber;
	}

	public void setMIN_INTERIORNODE_POINT_NUMBER(int mIN_INTERIORNODE_POINT_NUMBER) {
		min_interiorNode_pointNumber = mIN_INTERIORNODE_POINT_NUMBER;
	}
	
	
	
	
}
