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

	$query = "select password,nickname,items,win_num,lose_num,money from BattleYutUser where id = ?";
	$stmt = $db->prepare($query);
	$stmt->bind_param('s', $userID);
	$stmt->execute();	
	$stmt->store_result();
	$stmt->bind_result($searchPassword,$searchNickName,$searchItems,$winScore,$loseScore, $money);
	$index = $stmt->num_rows;
	$stmt->fetch();

	
	if($index>0){
	$response = array();		// 클라이언트가 전달 받을 데이터 배열 만들기.
	$response["idCheck"] = true;	//아이디가 있다는 사실을 변수에 입력

		if(strcmp($userPassword,$searchPassword)){	//사용자가 입력한 비밀번호가 틀린  경우
		$response["passwordCheck"] = false;		//비밀번호가 틀렸다는 사실을 변수에 입력
		}else{						//사용자가 입력한 비밀번호가 옳은 경우
		$response["passwordCheck"] = true;		//비밀번호가 옳다는 사실을 변수에 입력
		$response["nickname"] = $searchNickName;	//사용자의 닉네임을 변수에 입력
		$response["items"] = $searchItems;
		$response["winScore"] = $winScore;
		$response["loseScore"] = $loseScore;
		$response["money"] = $money;
		}
	
	}else{
	$response = array();	// 클라리언트가 받을 json 데이터 배열 만들기.
	$response["idCheck"] = false; //존재하지 않는 아이디라는 의미로 false 입력.
	}

	echo json_encode($response);	//클라이언트에게 json데이터 보내기
?>


