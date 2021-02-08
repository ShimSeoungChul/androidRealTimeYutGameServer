<?php

$host = "localhost";
$user = "BattleYut_Admin";
$pw = "BattleYut_Admin999995419!";
$dbName = "BattleYut";

@$db = new mysqli($host, $user, $pw, $dbName); //데이터베이스 연결

if(mysqli_connect_errno()){               /* 상품 정보 데이터 베이스에 연결되었나 확인하는 조건문*/
 echo '<p>Error: 데이터베이스 연결에 실패했습니다.</p>';}

	
	
	$userID = $_POST["userID"];
	//클라이언트에서 받아온 ㅂ트맵 파일을 변수에 입력

	////*비트맵  파일을 BattleYutProfile 디렉토리에 저장*////

	////*저장한 비트맵 파일 경로를 데이터베이스에 저장*//// 

	////*비트맵 저장 결과를 클라이언트에 전송*////



?>


