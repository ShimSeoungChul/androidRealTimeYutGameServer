<?php
$host = "localhost";
$user = "BattleYut_Admin";
$pw = "BattleYut_Admin999995419!";
$dbName = "BattleYut";

@$db = new mysqli($host, $user, $pw, $dbName);

if(mysqli_connect_errno()){               /* 상품 정보 데이터 베이스에 연결되었나 확인하는 조건문*/
 echo '<p>Error: 데이터베이스 연결에 실패했습니다.</p>';}

	
	
	$userID = $_POST["userID"];
	$winNum = intval($_POST["winNum"]);	

	$query = "update BattleYutUser set win_num = ? where id = ?";
	$stmt = $db->prepare($query);
	$stmt->bind_param('is', $winNum, $userID);
	$stmt->execute();	
	
	$response["success"]=true;	
	
	echo json_encode($response);	//클라이언트에게 json데이터 보내기
?>

