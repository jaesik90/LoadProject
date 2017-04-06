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
	Thread thread;//엑셀 등록시 사용될 쓰레드
	//왜?? 데이터량의 너무 많을경우, 네트워크
	//상태가 좋지 않을경우 insert가 while문 속도를
	//못따라간다...
	//따라서 안정성을 위해 일부러 시간 지연을 일으켜
	//insert 시도할거임
	
	//엑셀 파일에 의해 생성된 쿼리문을 쓰레드가
	//사용할 수 있는 상태로 저장해놓자!!
	StringBuffer insertSql=new StringBuffer();
	String seq;
	
	
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
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JTable t=(JTable) e.getSource();
				
				
				int row=t.getSelectedRow();
				int col=0; //seq는 첫번째 컬럼이니깐!!
				
				seq=(String) t.getValueAt(row, col);
				
			}
		});
		
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
	 		int result = chooser.showOpenDialog(this);
	 		//열기를 누르면 목적파일의 스트림을 생성하자.
	 		if(result==JFileChooser.APPROVE_OPTION){
	 			File file = chooser.getSelectedFile(); //유저가 선택한 파일
	 			String ext = FileUtil.getExt(file.getName());
	 			
	 			if(!ext.equals("csv")){
	 				JOptionPane.showMessageDialog(this, "CSV만 넣어주세요!");
	 				return;//진행막자
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
			StringBuffer cols = new StringBuffer();
			StringBuffer data = new StringBuffer();
			
			try {
				fis= new FileInputStream(file);
				
				HSSFWorkbook book=null;
			    book=new HSSFWorkbook(fis);//엑셀파일을 fis로 읽어들인다
			    
			    HSSFSheet sheet=null;
			    sheet = book.getSheet("동물병원");//동물병원 이라는 sheet를 book에서 얻어낸다...
			    
			    int total=sheet.getLastRowNum();// total변수에 sheet의 마지막 레코드 번호를 집어 넣는다
			   // System.out.println(rowNum);
			    DataFormatter df = new DataFormatter();// df라는 데이터 포맷터를 생성한다
			   
			    PreparedStatement pstmt=null;
			    String sql="";// 밑에 있는 sql문의 초기화... null과 같은 기능
				
				/*---------------------------------------------
				 *  첫번째 row는 데이터가 아닌 컬럼 정보이므로 
				 *  이정보들을 추출하여 insert into table(~~~~)에 넣자
				 * --------------------------------------------*/
			    
				System.out.println("이 파일의 첫번째 row번호는? " + sheet.getFirstRowNum());
				HSSFRow firstRow = sheet.getRow(sheet.getFirstRowNum());
				//Row을 얻었으니 컬럼을 분석하자
				firstRow.getLastCellNum(); //마지막 셀 넘버
				
				cols.delete(0, cols.length());//cols String버퍼의 처음 한번초기화..
				
				for(int i=0;i<firstRow.getLastCellNum();i++){
					HSSFCell cell = firstRow.getCell(i);
					if(i <firstRow.getLastCellNum()-1){
						cols.append(cell.getStringCellValue()+",");
						System.out.print(cell.getStringCellValue()+",");
					}else{
						
						cols.append(cell.getStringCellValue());
						System.out.print(cell.getStringCellValue());
					}
						
				}//여기까지 칼럼명 구하기...
				
				for(int i=1; i<=total;i++){
  					HSSFRow row =sheet.getRow(i);
  					int columnCount= row.getLastCellNum();
  					
 					
 					data.delete(0, data.length());
  					for(int j=0;j<columnCount;j++){
  						HSSFCell cell =row.getCell(j);
 						//자료형에 국한되지 않고 모두 String처리 할 수 있다.						
 						String value = df.formatCellValue(cell);
 						//자료형에 국한되지 않고 모두 String처리 할 수 있다.
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
				JOptionPane.showMessageDialog(this, "입력완료");
				//모든것이 끝낫으니 편안하게 쓰레드에게 일시키자!! 타이밍 문제 해결
				 			thread = new Thread(this); //쓰레드 자체의 run을 수행하는 것이 아니라.. 내 클래스의 run을 수행하게 된다!this을 넣는 이유(타켓!)
				 			thread.start();
			
				
				
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
		int ans=JOptionPane.showConfirmDialog(LoadMain.this, seq+"선택한 레코드 삭제할래요??");
		if(ans==JOptionPane.OK_OPTION){
			String sql="delete from hospital where seq="+seq;
			PreparedStatement pstmt=null;
			try {
				pstmt=con.prepareStatement(sql);
				int result =pstmt.executeUpdate();
				if(result!=0){
					JOptionPane.showMessageDialog(this, "삭제완료");
					table.updateUI(); //테이블 갱신
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

	//테이블 모델의 데이터값에 변결이 발생하면,
	//그 찰나를 감지하는 리스너!!
	public void tableChanged(TableModelEvent e) {
		int row=table.getSelectedRow();
		int col=table.getSelectedColumn();
		
		String column=(String) columnName.elementAt(col);
		
		//지정한 좌표의 값 반환
		String value=(String) table.getValueAt(row, col);
		
		String seq=(String) table.getValueAt(row, col);
		
		String sql="update hospital set "+column+"="+value+";";
		sql+="where seq="+seq;
		
		//쿼리문 실행!!
		PreparedStatement pstmt =null;
		try {
			pstmt=con.prepareStatement(sql);
			int result = pstmt.executeUpdate();
			if(result!=0){
				JOptionPane.showMessageDialog(this, "수정완료");
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
		//insertSql에 insert 문이 몇개인지 알아보자
		String[] str=insertSql.toString().split(";");
		System.out.println("insert문의 수는"+str.length);
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
				System.out.println("찍는중"+i+" 번째");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		//기존에 사용했던 StringBuffer 비우기
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
