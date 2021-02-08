<?php
$host = "localhost";
$user = "BattleYut_Admin";
$pw = "BattleYut_Admin999995419!";
$dbName = "BattleYut";

@$db = new mysqli($host, $user, $pw, $dbName);

if(mysqli_connect_errno()){               /* 상품 정보 데이터 베이스에 연결되었나 확인하는 조건문*/
 echo '<p>Error: 데이터베이스 연결에 실패했습니다.</p>';}

	
	
	$userID = $_POST["userID"];
	$ItemPrice = intVal($_POST["ItemPrice"]);
	$ItemList = $_POST["ItemList"];	

	$query = "select money from BattleYutUser where id = ?";
	$stmt = $db->prepare($query);
	$stmt->bind_param('s',$userID);
	$stmt->execute();
	$stmt->store_result();
	$stmt->bind_result($searchMoney);
	$index = $stmt->num_rows;
	$stmt->fetch();

	if($index>0){
	$searchMoney += $ItemPrice / 2;	//현재 보유한  게임머니에 아이템 가격의 1/2 만큼을 더한 게임버니를 DB에 저장한다.
 
	$query = "update BattleYutUser set items = ?, money = ? where id = ?";
	$stmt = $db->prepare($query);
	$stmt->bind_param('sis', $ItemList,$searchMoney,$userID);
	$stmt->execute();	
	
	$response["success"]=true;	
	}else{
	$response["success"] = false;
	}
	
	echo json_encode($response);	//클라이언트에게 json데이터 보내기
?>

