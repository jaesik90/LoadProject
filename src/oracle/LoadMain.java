package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.ArrayList;
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

public class LoadMain extends JFrame implements ActionListener, TableModelListener{
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
	
	//윈도우창이 열리면 이미 접속을 확보해놓자!!
	DBManager manager=DBManager.getInstance();
	Connection con;
	
	public LoadMain(){
		p_north = new JPanel();
		t_path =new JTextField(25);
		bt_open = new JButton("파일열기");
		bt_load = new JButton("로드하기");
		bt_excel = new JButton("엑셀로드");
		bt_del = new JButton("삭제하기");
		
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
		
		//윈도우와 리스너와 연결
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
		//	Connection 얻어다 놓기
		con=manager.getConnection();
		
		
	}
	
	//파일 탐색기 띄우기
	public void open(){
		int result=chooser.showOpenDialog(this);
		
		//열기를 누르면... 목적 파일에 스트림을 생성
		if(result==JFileChooser.APPROVE_OPTION){
			
			//유저가 선택한 파일!!
			File file=chooser.getSelectedFile();
			
			t_path.setText(file.getAbsolutePath());
			
			try {
				reader= new FileReader(file);
				buffr = new BufferedReader(reader);
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	//CSV--> Oracle로 데이터 이전(migration)하기
	public void load(){
		//버퍼스트림을 이용하여 csv의 데이터를
		//1줄씩 읽어들여 insert 시키자!!
		//레코드 없을때 까지...
		//while문으로 돌리면 너무 빠르므로,
		//네트워크가 감당할 수 없기 때문에 일부러
		//지연시키면서...
		String data;
		StringBuffer sb = new StringBuffer();
		PreparedStatement pstmt=null;
		try {
			while(true){
				data=buffr.readLine();
				
				if(data==null)break;
				
				//,은 특수문자여도 개발상의 기능이 없기때문에
				//이스케이프 //처리를 하지 않아도 된다
				String[] value=data.split(",");
				
				//seq 줄을 제외하고 insert 하겠다!!
				if(!value[0].equals("seq")){
					sb.append("insert into hospital(seq,name,addr,regdate,status,dimension,type)");
					sb.append(" values("+value[0]+",'"+value[1]+"','"+value[2]+"','"+value[3]+"','"+value[4]+"',"+value[5]+",'"+value[6]+"')");
					System.out.println(sb.toString());
					pstmt=con.prepareStatement(sb.toString());
					
					int result = pstmt.executeUpdate();
					//기존에 누적된 StringBuffer의
					//데이터를 모두 지우기
					sb.delete(0, sb.length());
				}else{
					System.out.println("난 첫줄이므로 제외");
				}
			}
			JOptionPane.showMessageDialog(this, "마이그레이션 완료!!");
			
			//JTable 나오게하기
			getList();
			table.setModel(new MyModel(list, columnName));
			table.updateUI();
			
			//테이블 모델과 리스너와의 연결
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
	
	//엑셀파일을 읽어서 db에 마이그레이션 하기!!
	//javaSE 엑셀제어 라이브러리 있다??X
	//open Source 공개소프트웨어
	//copyright <---> copyleft (아파치 단체)
	//POI 라이브러리! http://apache.org
	
	/*
	 * HSSFWorkbook : 엑셀파일
	 * HSSFSheet : sheet
	 * HSSFRow : row
	 * HSSFCell : cell
	 * */
	public void loadExcel(){
		int result=chooser.showOpenDialog(this);
		if(result==JFileChooser.APPROVE_OPTION){
			File file=chooser.getSelectedFile();
			FileInputStream fis=null;
			try {
				fis= new FileInputStream(file);
				
				HSSFWorkbook book=null;
			    book=new HSSFWorkbook(fis);
			    
			    HSSFSheet sheet=null;
			    sheet = book.getSheet("동물병원");
			    
			    int total=sheet.getLastRowNum();
			   // System.out.println(rowNum);
			    DataFormatter df = new DataFormatter();
			    StringBuffer sb= new StringBuffer();
			    ArrayList<String>list = new ArrayList<String>();
			    for(int a=1; a<=total; a++){
			    	
				    HSSFRow row=sheet.getRow(a);
				    int columnCount=row.getLastCellNum();
				 
				    for(int i=0; i<columnCount; i++){
					    HSSFCell cell= row.getCell(i);
					    //자료형에 국한되지 않고 모두 String 처리
					    String value=df.formatCellValue(cell);
					    //System.out.print(value);   
					   	list.add(value);
					   
				    }
					//System.out.println("list의 값은: "+list.get(3));
				    System.out.println("");
			    }
			    
			    
			    
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//모든 레코드 가져오기!!
	public void getList(){
		String sql="select * from hospital order by seq asc";
		
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			
			//컬럼명도 추출!!
			ResultSetMetaData meta= rs.getMetaData();
			int count = meta.getColumnCount();
			columnName = new Vector();
			for(int i=0; i<count;i++){
				columnName.add(meta.getColumnName(i+1));
			}
			
			list = new Vector<Vector>();//이차원 벡터
			while(rs.next()){
				
				Vector vec = new Vector(); //레코드 1건 담을거임
				
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
	
	
	//선택한 레코드 삭제
	public void delete(){
		
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

	//테이블 모델의 데이터값에 변결이 발생하면,
	//그 찰나를 감지하는 리스너!!
	public void tableChanged(TableModelEvent e) {
		System.out.println("나 바꿨어??");
		//내가 수정한 값을 찾아서 row,col값을 화면에 찍어주기!!
		//update hospital set 컬럼명=값 where seq값으로 업데이트 문으로 출력하는것 까지...
	}
	public static void main(String[] args) {
		new LoadMain();

	}

}
