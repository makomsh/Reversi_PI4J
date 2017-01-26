import java.net.*;
import java.io.*;
import javax.swing.*;
import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class MyClient extends JFrame implements MouseListener,MouseMotionListener {
	private JButton buttonArray[][];//ボタン用の配列
	private JButton passbutton;
	private int myColor;
	private int myTurn;
	private int click=0;
	private ImageIcon myIcon,yourIcon;
	private Container c;
	private ImageIcon blackIcon, whiteIcon, boardIcon;
	PrintWriter out;//出力用のライター

	public MyClient() {
		//名前の入力ダイアログを開く
		String myName = JOptionPane.showInputDialog(null,"名前を入力してください","名前の入力",JOptionPane.QUESTION_MESSAGE);
		if(myName.equals("")){
			myName = "No name";//名前がないときは，"No name"とする
		}
		
		String myIP = JOptionPane.showInputDialog(null,"IPアドレスを入力してください","アドレスの入力",JOptionPane.QUESTION_MESSAGE);
		if(myIP.equals("")){
			myIP = "localhost";//名前がないときは，"No name"とする
		}
		//ウィンドウを作成する
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//ウィンドウを閉じるときに，正しく閉じるように設定する
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent ex){
				out.println("bye");
				out.flush();			
			}		
		});
		setTitle("MyClient");//ウィンドウのタイトルを設定する
		setSize(500,500);//ウィンドウのサイズを設定する
		c = getContentPane();//フレームのペインを取得する

		//アイコンの設定
		whiteIcon = new ImageIcon("Black.jpg");
		blackIcon = new ImageIcon("White.jpg");
		boardIcon = new ImageIcon("GreenFrame.jpg");

		c.setLayout(null);//自動レイアウトの設定を行わない
		//ボタンの生成
		buttonArray = new JButton[8][8];//ボタンの配列を５個作成する[0]から[4]まで使える
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
				buttonArray[i][j] = new JButton(boardIcon);//ボタンにアイコンを設定する
				c.add(buttonArray[i][j]);//ペインに貼り付ける
				buttonArray[i][j].setBounds(i*45,j*45,45,45);//ボタンの大きさと位置を設定する．(x座標，y座標,xの幅,yの幅）
				buttonArray[i][j].addMouseListener(this);//ボタンをマウスでさわったときに反応するようにする
				buttonArray[i][j].addMouseMotionListener(this);//ボタンをマウスで動かそうとしたときに反応するようにする
				buttonArray[i][j].setActionCommand(Integer.toString(i+j*8));//ボタンに配列の情報を付加する（ネットワークを介してオブジェクトを識別するため）
			}
		}
		passbutton=new JButton("PASS");
		c.add(passbutton);
		passbutton.setBounds(370,50,100,45);//ボタンの大きさと位置を設定する．(x座標，y座標,xの幅,yの幅）
		passbutton.addMouseListener(this);//ボタンをマウスでさわったときに反応するようにする
		passbutton.setActionCommand("pass");
				
		buttonArray[3][3].setIcon(whiteIcon);
		buttonArray[4][4].setIcon(whiteIcon);
		buttonArray[3][4].setIcon(blackIcon);
		buttonArray[4][3].setIcon(blackIcon);
		//サーバに接続する
		Socket socket = null;
		try {
			//"localhost"は，自分内部への接続．localhostを接続先のIP Address（"133.42.155.201"形式）に設定すると他のPCのサーバと通信できる
			//10000はポート番号．IP Addressで接続するPCを決めて，ポート番号でそのPC上動作するプログラムを特定する
			socket = new Socket(myIP, 10000);
		} catch (UnknownHostException e) {
			System.err.println("ホストの IP アドレスが判定できません: " + e);
		} catch (IOException e) {
			 System.err.println("エラーが発生しました: " + e);
		}
		
		MesgRecvThread mrt = new MesgRecvThread(socket, myName);//受信用のスレッドを作成する
		mrt.start();//スレッドを動かす（Runが動く）
	}
		
	//メッセージ受信のためのスレッド
	public class MesgRecvThread extends Thread {
		
		Socket socket;
		String myName;
		
		public MesgRecvThread(Socket s, String n){
			socket = s;
			myName = n;
		}
		
		//通信状況を監視し，受信データによって動作する
		public void run() {
			try{
				
				InputStreamReader sisr = new InputStreamReader(socket.getInputStream());
				BufferedReader br = new BufferedReader(sisr);
				out = new PrintWriter(socket.getOutputStream(), true);
				out.println(myName);//接続の最初に名前を送る
				String myNumberStr=br.readLine();
				int myNumberInt=Integer.parseInt(myNumberStr);
				
				myTurn=0;
				
				if(myNumberInt%2==0){
					myColor=0;
					myIcon=blackIcon;
					yourIcon=whiteIcon;
				}
				else{
					myColor=1;
					myIcon=whiteIcon;
					yourIcon=blackIcon;
				}
				
				while(true) {
					String inputLine = br.readLine();//データを一行分だけ読み込んでみる
					if (inputLine != null) {//読み込んだときにデータが読み込まれたかどうかをチェックする
						System.out.println(inputLine);//デバッグ（動作確認用）にコンソールに出力する
						String[] inputTokens = inputLine.split(" ");	//入力データを解析するために、スペースで切り分ける
						String cmd = inputTokens[0];//コマンドの取り出し．１つ目の要素を取り出す
						/*if(cmd.equals("MOVE")){//cmdの文字と"MOVE"が同じか調べる．同じ時にtrueとなる
							//MOVEの時の処理(コマの移動の処理)
							String theBName = inputTokens[1];//ボタンの名前（番号）の取得
							int theBnum = Integer.parseInt(theBName);//ボタンの名前を数値に変換する
							int x = Integer.parseInt(inputTokens[2]);//数値に変換する
							int y = Integer.parseInt(inputTokens[3]);//数値に変換する
							buttonArray[theBnum].setLocation(x,y);//指定のボタンを位置をx,yに設定する
						}*/
						if(cmd.equals("CLICK")){//自分の色を相手の画面に反映
							System.out.println("MY TURN"+myTurn);
							myTurn=1-myTurn;//両者のターンを切り替え
							click=0;
							String theBName=inputTokens[1];
							int theBnum = Integer.parseInt(theBName);
							int Color=Integer.parseInt(inputTokens[2]);//送られてきたメッセージのMycolorの部分を取り出して
							int i=theBnum%8;
							int j=theBnum/8;
							/*if(Color==0){								//条件分岐で白か黒かきめる
							buttonArray[i][j].setIcon(blackIcon);
							}
							else{
								buttonArray[i][j].setIcon(whiteIcon);
							}*/
							if(Color==myColor){
								//送信元クライアントでの処理
								buttonArray[i][j].setIcon(yourIcon);
							}else{
								//送信先クライアントでの処理
								buttonArray[i][j].setIcon(myIcon);
								boolean judgeflag=true;
								for(int n=0;n<8;n++){
									for(int m=0;m<8;m++){
										if(buttonArray[n][m].getIcon()==boardIcon){
											judgeflag=false;
											break;
										}
									}
								}
								if(judgeflag){
									System.out.println("ゲーム終了です");
									//サーバーに送信
									String msg="JUDGE";
									out.println(msg);
									out.flush();
								}
							}
						}
						else if(cmd.equals("FLIP")){
							int x=Integer.parseInt(inputTokens[1]);
							int y=Integer.parseInt(inputTokens[2]);
							int Color=Integer.parseInt(inputTokens[3]);
							
							if(Color==myColor){
								buttonArray[x][y].setIcon(yourIcon);
							}else{
								buttonArray[x][y].setIcon(myIcon);
							}
						}
						else if(cmd.equals("PASS")){
							click++;
							myTurn=1-myTurn;
							if(click>=3){
								System.out.println("パス回数上限のためゲーム終了です");
								int color=Integer.parseInt(inputTokens[1]);
								if(color==myColor){
									//サーバーに送信
									String msg="JUDGE";
									out.println(msg);
									out.flush();
								}
							}
							
						}
						else if(cmd.equals("JUDGE")){
							int whitecount=0;
							int blackcount=0;
							for(int n=0;n<8;n++){
								for(int m=0;m<8;m++){
									if(buttonArray[n][m].getIcon()==whiteIcon){
										whitecount++;
									}else if(buttonArray[n][m].getIcon()==blackIcon){
										blackcount++;
									}
								}
							}
							if(whitecount>blackcount){
								System.out.println("白＝"+whitecount+"個");
								System.out.println("黒＝"+blackcount+"個");
								System.out.println("白の勝ち");
								
							}else if(whitecount<blackcount){
								
								System.out.println("白＝"+whitecount+"個");
								System.out.println("黒＝"+blackcount+"個");
								System.out.println("黒の勝ち");
							}else{
								System.out.println("引き分け");
							}
						}
					}else{
						break;
					}
				}
				socket.close();
			} catch (IOException e) {
				System.err.println("エラーが発生しました: " + e);
			}
		}
	}

	public static void main(String[] args) {
		MyClient net = new MyClient();
		net.setVisible(true);
	}
  	
	public void mouseClicked(MouseEvent e) {//ボタンをクリックしたときの処理
		System.out.println("クリック");
		System.out.println("myTurn="+myTurn);
		if(myColor==myTurn){
			JButton theButton = (JButton)e.getComponent();//クリックしたオブジェクトを得る．型が違うのでキャストする
			String theArrayIndex = theButton.getActionCommand();//ボタンの配列の番号を取り出す
			if(theArrayIndex.equals("pass")){
				String msg="PASS"+" "+myColor;
				
				out.println(msg);
				out.flush();
				
				repaint();
			}
			else{
				Icon theIcon = theButton.getIcon();//theIconには，現在のボタンに設定されたアイコンが入る
				System.out.println(theIcon);//デバッグ（確認用）に，クリックしたアイコンの名前を出力する
				if(theIcon==boardIcon){
					int index=Integer.parseInt(theArrayIndex);
					if(judgeButton(index)){
						/*if(myColor==0){
							theButton.setIcon(blackIcon);
						}
						else{
							theButton.setIcon(whiteIcon);
						}*/
						//送信情報を作成する（受信時には，この送った順番にデータを取り出す．スペースがデータの区切りとなる）
						String msg = "CLICK"+" "+theArrayIndex+" "+myColor;

						//サーバに情報を送る
						out.println(msg);//送信データをバッファに書き出す
						out.flush();//送信データをフラッシュ（ネットワーク上にはき出す）する
						
						repaint();//画面のオブジェクトを描画し直す
					}else{
						System.out.println("そこには配置できません");
					}
				}
			}
		}
	}
	
	public void mouseEntered(MouseEvent e) {//マウスがオブジェクトに入ったときの処理
		//System.out.println("マウスが入った");
	}
	
	public void mouseExited(MouseEvent e) {//マウスがオブジェクトから出たときの処理
		//System.out.println("マウス脱出");
	}
	
	public void mousePressed(MouseEvent e) {//マウスでオブジェクトを押したときの処理（クリックとの違いに注意）
		//System.out.println("マウスを押した");
	}
	
	public void mouseReleased(MouseEvent e) {//マウスで押していたオブジェクトを離したときの処理
		//System.out.println("マウスを放した");
	}
	
	public void mouseDragged(MouseEvent e) {//マウスでオブジェクトとをドラッグしているときの処理
		//System.out.println("マウスをドラッグ");
		JButton theButton = (JButton)e.getComponent();//型が違うのでキャストする
		String theArrayIndex = theButton.getActionCommand();//ボタンの配列の番号を取り出す

		if(theArrayIndex.equals("64")){
			Point theMLoc = e.getPoint();//発生元コンポーネントを基準とする相対座標
			System.out.println(theMLoc);//デバッグ（確認用）に，取得したマウスの位置をコンソールに出力する
			Point theBtnLocation = theButton.getLocation();//クリックしたボタンを座標を取得する
			theBtnLocation.x += theMLoc.x-15;//ボタンの真ん中当たりにマウスカーソルがくるように補正する
			theBtnLocation.y += theMLoc.y-15;//ボタンの真ん中当たりにマウスカーソルがくるように補正する
			theButton.setLocation(theBtnLocation);//マウスの位置にあわせてオブジェクトを移動する
			
			//送信情報を作成する（受信時には，この送った順番にデータを取り出す．スペースがデータの区切りとなる）
			String msg = "MOVE"+" "+theArrayIndex+" "+theBtnLocation.x+" "+theBtnLocation.y;

			//サーバに情報を送る
			out.println(msg);//送信データをバッファに書き出す
			out.flush();//送信データをフラッシュ（ネットワーク上にはき出す）する

			repaint();//オブジェクトの再描画を行う
		}
	}
		


	public void mouseMoved(MouseEvent e) {}//マウスがオブジェクト上で移動したときの処理
	
	public boolean judgeButton(int index){
		boolean flag=false;
		System.out.println("myColor="+myColor);
		System.out.println("myIcon="+myIcon);
		System.out.println("yourIcon="+yourIcon);
		//いろいろな条件からflagをtrueにするか判断する
		int i=index%8;
		int j=index/8;
		for(int k=-1;k<2;k++){
			for(int l=-1;l<2;l++){
				System.out.println("i+k="+(i+k));
				System.out.println("j+l="+(j+l));
				if((l!=0||k!=0)&&0<=i+k&&i+k<8&&0<=l+j&&l+j<8){
					System.out.println("Icon="+buttonArray[i+k][j+l].getIcon() + " " + yourIcon);
					if(buttonArray[i+k][j+l].getIcon()==myIcon&&flipButton(index,k,l)>0){
					
					//if(buttonArray[i+k][j+l].getIcon()==yourIcon){
						flag=true;
						System.out.println("flag="+flag);
					}
				}
			}
		}
		
		return flag;
	}
	
	public int flipButton(int index,int i,int j){
		int flipNum=0;
		int dx=i;
		int dy=j;
		int x=index%8;
		int y=index/8;
		
		System.out.println("In flipButton");
		System.out.println("x+dx="+(x+dx));
		System.out.println("y+dy="+(y+dy));
				
		while(true){
			if((x+dx>7||y+dy>7||x+dx<0||y+dy<0)){
				flipNum=0;
				break;
			}
			Icon mark=buttonArray[x+dx][y+dy].getIcon();
			if(mark==boardIcon){
				flipNum=0;
				break;
			}else if(mark==yourIcon){
				break;
			}else if(mark==myIcon){
				flipNum++;
			}
			
			System.out.println("mark="+mark+" "+dx+" "+dy);
			
			dx=dx+i;
			dy=dy+j;
		}
		
		if(flipNum>=1){
			dx=i;
			dy=j;
			
			for(int k=0;k<flipNum;k++,dx+=i,dy+=j){
				//ボタンの位置情報
				int msgx=x+dx;
				int msgy=y+dy;
				
				//サーバーに送信
				String msg="FLIP"+" "+msgx+" "+msgy+" "+myColor;
				out.println(msg);
				out.flush();
			}
		}
		return flipNum;
	}
}

