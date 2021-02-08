<?php
$host = "localhost";
$user = "BattleYut_Admin";
$pw = "BattleYut_Admin999995419!";
$dbName = "BattleYut";

@$db = new mysqli($host, $user, $pw, $dbName);

if(mysqli_connect_errno()){               /* 상품 정보 데이터 베이스에 연결되었나 확인하는 조건문*/
 echo '<p>Error: 데이터베이스 연결에 실패했습니다.</p>';}

	
	
	$userID = $_POST["userID"];
	$userPassword = $_POST["userPassword"];
	$userNickName = $_POST["userNickName"];
	$score = (int)0;

	$query = "select id from BattleYutUser where id = ?";
	$stmt = $db->prepare($query);
	$stmt->bind_param('s', $userID);
	$stmt->execute();	
	$stmt->store_result();
	$stmt->bind_result($searchID);
	$index = $stmt->num_rows;
	
	if($index>0){
	$response = array();
	$response["idCheck"] = false;
	
	}else{
	$response = array();
	$response["idCheck"] = true;

	$query = "select nickname from BattleYutUser where nickname = ?";
	$stmt = $db->prepare($query);
	$stmt->bind_param('s', $userNickName);
	$stmt->execute();	
	$stmt->store_result();
	$stmt->bind_result($searchNickName);
	$index = $stmt->num_rows;

		if($index>0){
		$response["nickNameCheck"] = false;
		
		}else{
		$response["nickNameCheck"] = true;
	
		$query ="insert into BattleYutUser VALUES (null, ?, ?, ?, ?, ?, default, default)";	
		$stmt = $db->prepare($query);
		$stmt->bind_param('sssii',$userID,$userNickName,$userPassword,$score,$score);
		$stmt->execute();
	
		$response["success"] = true;

	
		}
	
	}

	$db->close();
	echo json_encode($response);
?>

