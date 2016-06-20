package dhu.btree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 模拟测试的结果会受到n,recordNum,blockSize的影响
 * 
 * 假设没有缓冲区，这对使用BTree和不使用BTree来说是公平的。那么假设每查找一个key就要读一个块，即进行一次I/O操作
 * 
 */
public class Transaction {
	int n = 4;			//bTree的参数，可以设置 
	//int DB[]={5,13,9,12,16, 7,11,23,2,91, 10,6, 17,24, 25, 50,26,1};	//数据库，只保存了键
	List<Integer> dbList = new ArrayList<Integer>();					//用list保存的数据库，只保存了键
	int recordNum = 20;			//模拟数据库中的初始记录数
	int blockSize = 10;	//一个块可以存储多少条记录
	int blockNum = (recordNum%blockSize==0 ? recordNum/blockSize : recordNum/blockSize+1);	//存储数据库用到多少块
	BTree bTree;
	
	List<Integer> listDeleteIndex = new ArrayList<Integer>();  //保存数据库dbList中已删除元素的下标-->没用到
	
	//为了保证数据库中的元素不变，把该类设为单例模式，否则每运行一次都要随机生产一次数据库元素
	//但是试了一下好像并没有达到想要的效果
	private Transaction() {
		init();
		buidTree();
	}
	private static Transaction instance = new Transaction();
	public static Transaction getInstance(){
		return instance;
	}
	
	public void init(){
		//随机生成recordNum条记录数，存到数据库中
		Random random = new Random();
		for(int i =0;i< recordNum;i++){		
			int record = Math.abs(random.nextInt()%(recordNum+10));
			while(dbList.contains(record)){			//保证key都不一样
				record = Math.abs(random.nextInt()%(recordNum*10));
			}
			dbList.add(record);
			
		}
	}
	
	//对数据库dbList建立BTree
	public void buidTree(){
		bTree = new BTree(n);
		bTree.buidTree(dbList);
		
	}
	
	//输出数据库中的所有数据
	public void printAllRecordInDB(){
		for(int i=0; i< dbList.size();i++){
			if(dbList.get(i)!=null){
				System.out.print("["+i+"]"+dbList.get(i)+" ");
			}
		}
		System.out.println();
		
	}
	
	//使用BTree查找一条记录
	public void findWithBTree(int record){
		System.out.println("\n-- Try to findWithBTree:"+record+" -->   IO number: "+(bTree.getBTreeHeight()+1));
		int res = bTree.searchKey(bTree.getRootId(), record);
		if(res != -1){
			System.out.println(res);
		}
	}
	
	//不使用BTree查找一条记录
	public void findWithoutBTree(int record){	
		int IONumber = 0;	//I/O次数
		int i = 0;
		for(i = 0;i<dbList.size();i++){
			if(dbList.get(i)!= null){	//为null表示这个位置的数据已经被删除，但为了方便，这个位置依然存在
				IONumber++;
				if(dbList.get(i) == record){
					break;
				}
			}
		}
		System.out.println("\n-- Try to findWithoutBTree:"+record+" -->   IO number: "+IONumber);
		if(i >= dbList.size()){	
			System.out.println("The key "+record+" is not exists.");
		}else{
			System.out.println(i);
		}
	}
	
	//使用BTree查找一个范围内的记录
	public void findRangeWithBTree(int fromRecord, int toRecord){
		System.out.print("\n-- Try to findRangeWithBTree: ["+fromRecord+" "+toRecord+"]"+" -->");
		List<Integer> res =  bTree.searchRange(fromRecord, toRecord);
		if(res.size()==0){
			System.out.println("Find nothing.");
			return ;
		}
		System.out.print("[");
		for(int i: res){
			System.out.print(i+" ");
		}
		System.out.println("]");
	}
	
	//不使用BTree查找一个范围内的记录
	public void findRangeWithoutBTree(int fromRecord, int toRecord){
		int IONumber = 0;
		List<Integer> res = new ArrayList<Integer>();
		for(int i=0;i<dbList.size();i++){
			if(dbList.get(i) != null){
				IONumber++;
				if(dbList.get(i) >= fromRecord && dbList.get(i) <= toRecord){
					res.add(i);
				}
				
			}
		}
		//输出查询到的结果
		System.out.println("\n-- Try to findRangeWithoutBTree: ["+fromRecord+" "+toRecord+"]"+" -->   IO number: "+IONumber);
		if(res.size()==0){
			System.out.println("Find nothing.");
			return ;
		}
		System.out.print("[");
		for(int i: res){
			System.out.print(i+" ");
		}
		System.out.println("]");
	}
	
	//使用BTree删除一条记录
	public int deleteWithBTree(int record){	//返回的是该记录的地址，即下标
		System.out.println("\n-- Try to deleteWithBTree:"+record+" -->   IO number: "+(bTree.getBTreeHeight()+1));
		int res = bTree.delete(record);
		if(res != -1){	
			System.out.println("Delete key "+ record + " in BTree succeed. The address in DB is "+dbList.indexOf(record)+".");
			return -1;
		}
		return dbList.indexOf(record);
	}
	
	//不使用BTree删除一条记录
	public int deleteWithoutBTree(int record){
		int IONumber = 0;	//I/O次数
		int i = 0;
		for(i = 0;i<dbList.size();i++){
			if(dbList.get(i)!= null){	//为null表示这个位置的数据已经被删除，但为了方便，这个位置依然存在
				IONumber++;
				if(dbList.get(i) == record){
					break;
				}
			}
		}
		System.out.println("\n-- Try to deleteWithoutBTree:"+record+" -->   IO number: "+IONumber);
		if(i >= dbList.size()){	
			System.out.println("The key "+record+" is not exists.");
			return -1;
		}else{
			System.out.println("Find "+record+", the address(value) is "+i+", you should delete it manually.");
			return i;
		}
	}
	
	//删除数据库中地址为address的记录
	public void deleteRecordInDB(int address){
		dbList.set(address, null);
	}
	
	//向数据库中增加一条记录,返回添加到数据库后该record的地址
	public int addRecordInDB(int record){
		dbList.add(record);
		return dbList.size()-1;
	}
	
	public static void main(String[] args) {
		
		Transaction transaction = Transaction.getInstance();
		
		transaction.printAllRecordInDB();
		transaction.bTree.printTree();
		int record;
		
		//查找一条记录
		record = 11;
		transaction.findWithBTree(record);
		transaction.findWithoutBTree(record);
		
		
		//范围查找
		int fromRecord = 3;
		int toRecord = 15;
		transaction.findRangeWithBTree(fromRecord, toRecord);
		transaction.findRangeWithoutBTree(fromRecord, toRecord);
		
		
		//删除一条记录
		record = 8;
		int address1 = transaction.deleteWithBTree(record);
		int address2 = transaction.deleteWithoutBTree(record);
		
		/*  为了上面两个删除的对比，这里没有对数据库中的元素进行真实的删除
		//删除数据库中的记录
		if(address1 != -1){
			transaction.deleteRecordInDB(address1);
		}
		if(address2 != -1){
			transaction.deleteRecordInDB(address2);
		}*/
		
		//向数据库中插入一条数据，并向BTree中添加一个键-值对
		int newKey  = 100;
		int value = transaction.addRecordInDB(newKey);
		transaction.bTree.insert(newKey, value);
		transaction.bTree.printTree();
		
		
		//删除数据库中的所有记录，演示BTree在有删除时的动态变化过程
		for(int i=0; i< transaction.dbList.size();i++){
			if(transaction.dbList.get(i)!=null){
				transaction.deleteWithBTree(transaction.dbList.get(i));
				transaction.bTree.printTree();
			}
		}
		
		
	}
}
