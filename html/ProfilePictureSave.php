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
	$ProfileBit = $_POST["ProfileBit"];

	////*비트맵  파일을 BattleYutProfile 디렉토리에 저장*////
	$upload_path = './BattleYutProfile/'.$userID; //저장할 파일 경로 + 비트맵파일이름(사용자의 아이디를 비트맵 파일 이름으로 한다.)

	if(file_put_contents($upload_path,base64_decode($ProfileBit))){
		//상품 이미지를 저장
		$response["uploadCheck"] = true;
	
		//=============데이터베이스에 상품 이미지 저장 여부를 표시한다===========
		$value = true; //데이터베이스 photo 필드에 입력할 값
		$query = "update BattleYutUser set photo = ? where id = ?";
		$stmt = $db->prepare($query);
		$stmt->bind_param('is', $value, $userID);
		$stmt->execute();

	}else{
		//상품 이미지 저장 실패
		$response["uploadCheck"] = false;
	}

	////*비트맵 저장 결과를 클라이언트에 전송*////
	echo json_encode($response);
		

?>


