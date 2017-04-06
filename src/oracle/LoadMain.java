package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

import util.file.FileUtil;

public class LoadMain extends JFrame implements ActionListener, TableModelListener, Runnable{
	JPanel p_north;
	JTextField t_path;
	JButton bt_open, bt_load, bt_excel ,bt_del;
	JTable table;
	JScrollPane scroll;
	JFileChooser chooser;
	FileReader reader=null;
	BufferedReader buffr=null;
	Vector<Vector> list;
	Vector columnName;
	MyModel myModel;
	Thread thread;//���� ��Ͻ� ���� ������
	//��?? �����ͷ��� �ʹ� �������, ��Ʈ��ũ
	//���°� ���� ������� insert�� while�� �ӵ���
	//�����󰣴�...
	//���� �������� ���� �Ϻη� �ð� ������ ������
	//insert �õ��Ұ���
	
	//���� ���Ͽ� ���� ������ �������� �����尡
	//����� �� �ִ� ���·� �����س���!!
	StringBuffer insertSql=new StringBuffer();
	String seq;
	
	
	//������â�� ������ �̹� ������ Ȯ���س���!!
	DBManager manager=DBManager.getInstance();
	Connection con;
	
	public LoadMain(){
		p_north = new JPanel();
		t_path =new JTextField(25);
		bt_open = new JButton("���Ͽ���");
		bt_load = new JButton("�ε��ϱ�");
		bt_excel = new JButton("�����ε�");
		bt_del = new JButton("�����ϱ�");
		
		table = new JTable();
		scroll = new JScrollPane(table);
		chooser = new JFileChooser("C:/animal");
		
		p_north.add(t_path);
		p_north.add(bt_open);
		p_north.add(bt_load);
		p_north.add(bt_excel);
		p_north.add(bt_del);
		
		add(p_north, BorderLayout.NORTH);
		add(scroll);
		
		bt_open.addActionListener(this);
		bt_load.addActionListener(this);
		bt_del.addActionListener(this);
		bt_excel.addActionListener(this);
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JTable t=(JTable) e.getSource();
				
				
				int row=t.getSelectedRow();
				int col=0; //seq�� ù��° �÷��̴ϱ�!!
				
				seq=(String) t.getValueAt(row, col);
				
			}
		});
		
		//������� �����ʿ� ����
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				manager.disConnection(con);
				System.exit(0);
			}
			
		});
		
		setVisible(true);
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		init();
		
	}
	public void init(){
		//	Connection ���� ����
		con=manager.getConnection();
		
		
	}
	
	//���� Ž���� ����
	 	public void open(){
	 		int result = chooser.showOpenDialog(this);
	 		//���⸦ ������ ���������� ��Ʈ���� ��������.
	 		if(result==JFileChooser.APPROVE_OPTION){
	 			File file = chooser.getSelectedFile(); //������ ������ ����
	 			String ext = FileUtil.getExt(file.getName());
	 			
	 			if(!ext.equals("csv")){
	 				JOptionPane.showMessageDialog(this, "CSV�� �־��ּ���!");
	 				return;//���ื��
	 			}else{
	 				t_path.setText(file.getAbsolutePath());
	 				try {
	 					reader =new FileReader(file);
	 					buffr = new BufferedReader(reader);
	 				} catch (FileNotFoundException e) {
	 					e.printStackTrace();
	 				}			
	 			}
	 		}
	 	}
	
	//CSV--> Oracle�� ������ ����(migration)�ϱ�
	public void load(){
		//���۽�Ʈ���� �̿��Ͽ� csv�� �����͸�
		//1�پ� �о�鿩 insert ��Ű��!!
		//���ڵ� ������ ����...
		//while������ ������ �ʹ� �����Ƿ�,
		//��Ʈ��ũ�� ������ �� ���� ������ �Ϻη�
		//������Ű�鼭...
		String data;
		StringBuffer sb = new StringBuffer();
		PreparedStatement pstmt=null;
		try {
			while(true){
				data=buffr.readLine();
				
				if(data==null)break;
				
				//,�� Ư�����ڿ��� ���߻��� ����� ���⶧����
				//�̽������� //ó���� ���� �ʾƵ� �ȴ�
				String[] value=data.split(",");
				
				//seq ���� �����ϰ� insert �ϰڴ�!!
				if(!value[0].equals("seq")){
					sb.append("insert into hospital(seq,name,addr,regdate,status,dimension,type)");
					sb.append(" values("+value[0]+",'"+value[1]+"','"+value[2]+"','"+value[3]+"','"+value[4]+"',"+value[5]+",'"+value[6]+"')");
					System.out.println(sb.toString());
					pstmt=con.prepareStatement(sb.toString());
					
					int result = pstmt.executeUpdate();
					//������ ������ StringBuffer��
					//�����͸� ��� �����
					sb.delete(0, sb.length());
				}else{
					System.out.println("�� ù���̹Ƿ� ����");
				}
			}
			JOptionPane.showMessageDialog(this, "���̱׷��̼� �Ϸ�!!");
			
			//JTable �������ϱ�
			getList();
			table.setModel(new MyModel(list, columnName));
			table.updateUI();
			
			//���̺� �𵨰� �����ʿ��� ����
			table.getModel().addTableModelListener(this);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	//���������� �о db�� ���̱׷��̼� �ϱ�!!
	//javaSE �������� ���̺귯�� �ִ�??X
	//open Source ��������Ʈ����
	//copyright <---> copyleft (����ġ ��ü)
	//POI ���̺귯��! http://apache.org
	
	/*
	 * HSSFWorkbook : ��������
	 * HSSFSheet : sheet
	 * HSSFRow : row
	 * HSSFCell : cell
	 * */
	public void loadExcel(){
		int result=chooser.showOpenDialog(this);
		if(result==JFileChooser.APPROVE_OPTION){
			File file=chooser.getSelectedFile();
			FileInputStream fis=null;
			StringBuffer cols = new StringBuffer();
			StringBuffer data = new StringBuffer();
			
			try {
				fis= new FileInputStream(file);
				
				HSSFWorkbook book=null;
			    book=new HSSFWorkbook(fis);//���������� fis�� �о���δ�
			    
			    HSSFSheet sheet=null;
			    sheet = book.getSheet("��������");//�������� �̶�� sheet�� book���� ����...
			    
			    int total=sheet.getLastRowNum();// total������ sheet�� ������ ���ڵ� ��ȣ�� ���� �ִ´�
			   // System.out.println(rowNum);
			    DataFormatter df = new DataFormatter();// df��� ������ �����͸� �����Ѵ�
			   
			    PreparedStatement pstmt=null;
			    String sql="";// �ؿ� �ִ� sql���� �ʱ�ȭ... null�� ���� ���
				
				/*---------------------------------------------
				 *  ù��° row�� �����Ͱ� �ƴ� �÷� �����̹Ƿ� 
				 *  ���������� �����Ͽ� insert into table(~~~~)�� ����
				 * --------------------------------------------*/
			    
				System.out.println("�� ������ ù��° row��ȣ��? " + sheet.getFirstRowNum());
				HSSFRow firstRow = sheet.getRow(sheet.getFirstRowNum());
				//Row�� ������� �÷��� �м�����
				firstRow.getLastCellNum(); //������ �� �ѹ�
				
				cols.delete(0, cols.length());//cols String������ ó�� �ѹ��ʱ�ȭ..
				
				for(int i=0;i<firstRow.getLastCellNum();i++){
					HSSFCell cell = firstRow.getCell(i);
					if(i <firstRow.getLastCellNum()-1){
						cols.append(cell.getStringCellValue()+",");
						System.out.print(cell.getStringCellValue()+",");
					}else{
						
						cols.append(cell.getStringCellValue());
						System.out.print(cell.getStringCellValue());
					}
						
				}//������� Į���� ���ϱ�...
				
				for(int i=1; i<=total;i++){
  					HSSFRow row =sheet.getRow(i);
  					int columnCount= row.getLastCellNum();
  					
 					
 					data.delete(0, data.length());
  					for(int j=0;j<columnCount;j++){
  						HSSFCell cell =row.getCell(j);
 						//�ڷ����� ���ѵ��� �ʰ� ��� Stringó�� �� �� �ִ�.						
 						String value = df.formatCellValue(cell);
 						//�ڷ����� ���ѵ��� �ʰ� ��� Stringó�� �� �� �ִ�.
 						if(cell.getCellType()==HSSFCell.CELL_TYPE_STRING){
 							value="'"+value+"'";
 						}
 						
  						if(j<columnCount-1){	
 							data.append(value+",");
  						}else{
 							data.append(value);
  						}
  					}
  					sql="insert into hospital("+cols.toString()+") values("+data.toString()+");";
  					insertSql.append(sql);
	
					//System.out.println(sql);
					//System.out.println("");
				}				
				JOptionPane.showMessageDialog(this, "�Է¿Ϸ�");
				//������ �������� ����ϰ� �����忡�� �Ͻ�Ű��!! Ÿ�̹� ���� �ذ�
				 			thread = new Thread(this); //������ ��ü�� run�� �����ϴ� ���� �ƴ϶�.. �� Ŭ������ run�� �����ϰ� �ȴ�!this�� �ִ� ����(Ÿ��!)
				 			thread.start();
			
				
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	//��� ���ڵ� ��������!!
	public void getList(){
		String sql="select * from hospital order by seq asc";
		
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			
			//�÷��� ����!!
			ResultSetMetaData meta= rs.getMetaData();
			int count = meta.getColumnCount();
			columnName = new Vector();
			for(int i=0; i<count;i++){
				columnName.add(meta.getColumnName(i+1));
			}
			
			list = new Vector<Vector>();//������ ����
			while(rs.next()){
				
				Vector vec = new Vector(); //���ڵ� 1�� ��������
				
				vec.add(rs.getString("seq"));
				vec.add(rs.getString("name"));
				vec.add(rs.getString("addr"));
				vec.add(rs.getString("regdate"));
				vec.add(rs.getString("status"));
				vec.add(rs.getString("dimension"));
				vec.add(rs.getString("type"));
				list.add(vec);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(rs!=null){
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	
	//������ ���ڵ� ����
	public void delete(){
		int ans=JOptionPane.showConfirmDialog(LoadMain.this, seq+"������ ���ڵ� �����ҷ���??");
		if(ans==JOptionPane.OK_OPTION){
			String sql="delete from hospital where seq="+seq;
			PreparedStatement pstmt=null;
			try {
				pstmt=con.prepareStatement(sql);
				int result =pstmt.executeUpdate();
				if(result!=0){
					JOptionPane.showMessageDialog(this, "�����Ϸ�");
					table.updateUI(); //���̺� ����
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}finally{
				if(pstmt!=null){
					try {
						pstmt.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		Object obj=e.getSource();
		if(obj==bt_open){
			open();
		}else if(obj==bt_load){
			load();
		}else if(obj==bt_excel){
			loadExcel();
		}else if(obj==bt_del){
			delete();
		}
	}

	//���̺� ���� �����Ͱ��� ������ �߻��ϸ�,
	//�� ������ �����ϴ� ������!!
	public void tableChanged(TableModelEvent e) {
		int row=table.getSelectedRow();
		int col=table.getSelectedColumn();
		
		String column=(String) columnName.elementAt(col);
		
		//������ ��ǥ�� �� ��ȯ
		String value=(String) table.getValueAt(row, col);
		
		String seq=(String) table.getValueAt(row, col);
		
		String sql="update hospital set "+column+"="+value+";";
		sql+="where seq="+seq;
		
		//������ ����!!
		PreparedStatement pstmt =null;
		try {
			pstmt=con.prepareStatement(sql);
			int result = pstmt.executeUpdate();
			if(result!=0){
				JOptionPane.showMessageDialog(this, "�����Ϸ�");
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}finally{
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	public void run(){
		//insertSql�� insert ���� ����� �˾ƺ���
		String[] str=insertSql.toString().split(";");
		System.out.println("insert���� ����"+str.length);
		PreparedStatement pstmt = null;
		
		for(int i=0; i<str.length; i++){
			//System.out.println(str[i]);
			try {
				thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				pstmt=con.prepareStatement(str[i]);
				int result = pstmt.executeUpdate();
				System.out.println("�����"+i+" ��°");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		//������ ����ߴ� StringBuffer ����
		insertSql.delete(0, insertSql.length());
		if(pstmt!=null){
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args) {
		new LoadMain();

	}

}
