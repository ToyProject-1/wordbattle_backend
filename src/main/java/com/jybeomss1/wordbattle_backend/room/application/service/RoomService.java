package com.jybeomss1.wordbattle_backend.room.application.service;

import com.jybeomss1.wordbattle_backend.common.exceptions.BaseException;
import com.jybeomss1.wordbattle_backend.common.exceptions.ErrorCode;
import com.jybeomss1.wordbattle_backend.game.domain.GameStatus;
import com.jybeomss1.wordbattle_backend.room.application.port.in.RoomCreateUseCase;
import com.jybeomss1.wordbattle_backend.room.application.port.in.RoomDetailUseCase;
import com.jybeomss1.wordbattle_backend.room.application.port.in.RoomJoinUseCase;
import com.jybeomss1.wordbattle_backend.room.application.port.in.RoomListUseCase;
import com.jybeomss1.wordbattle_backend.room.application.port.out.RoomPort;
import com.jybeomss1.wordbattle_backend.room.domain.Room;
import com.jybeomss1.wordbattle_backend.room.domain.RoomUser;
import com.jybeomss1.wordbattle_backend.room.domain.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService implements RoomCreateUseCase, RoomJoinUseCase, RoomListUseCase, RoomDetailUseCase {
    private final RoomPort roomPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public RoomCreateResponse createRoom(RoomCreateRequest request, UUID userId, String name) {
        // 1. 방장 유저 정보 생성
        RoomUser hostUser = RoomUser.fromHostInfo(userId.toString(), name);
        // 2. PasswordEncoder로 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // 3. Room 도메인 객체 생성
        Room room = request.toEntity(hostUser, encodedPassword);
        // 4. 저장
        Room saved = roomPort.save(room);
        // 5. 응답 변환 (간단히 room 정보만 반환)
        return RoomCreateResponse.from(saved);
    }

    @Override
    @Transactional
    public RoomJoinResponse joinRoom(RoomJoinRequest request, UUID userId, String name) {
        // 1. 방 조회
        Room room = roomPort.findById(request.getRoomId())
                .orElseThrow(() -> new BaseException(ErrorCode.ROOM_NOT_FOUND));
        // 2. 인원 제한 체크
        if (room.getUsers() != null && room.getUsers().size() >= 10) {
            throw new BaseException(ErrorCode.ROOM_FULL);
        }
        // 3. 비밀번호 체크(필요시)
        if (room.isHasPassword()) {
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new BaseException(ErrorCode.ROOM_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(request.getPassword(), room.getPassword())) {
                throw new BaseException(ErrorCode.ROOM_PASSWORD_MISMATCH);
            }
        }
        // 4. 참가자 RoomUser 생성
        RoomUser joinUser = RoomUser.fromJoinInfo(userId.toString(), name);
        // 5. Room의 users에 추가
        Room updatedRoom = Room.fromJoin(room, joinUser);
        // 6. 저장
        roomPort.save(updatedRoom);
        // 7. 응답 변환
        return RoomJoinResponse.from(updatedRoom);
    }


    @Override
    @Transactional
    public RoomJoinResponse joinRoomByJoinCode(String joinCode, UUID userId, String name) {
        // 1. joinCode로 방 조회
        Room room = roomPort.findByJoinCode(joinCode)
                .orElseThrow(() -> new BaseException(ErrorCode.ROOM_NOT_FOUND));
        // 2. 인원 제한 체크
        if (room.getUsers() != null && room.getUsers().size() >= 10) {
            throw new BaseException(ErrorCode.ROOM_FULL);
        }
        // 3. 참가자 RoomUser 생성
        RoomUser joinUser = RoomUser.fromJoinInfo(userId.toString(), name);
        // 4. Room의 users에 추가
        Room updatedRoom = Room.fromJoin(room, joinUser);
        // 5. 저장
        roomPort.save(updatedRoom);
        // 6. 응답 변환
        return RoomJoinResponse.from(updatedRoom);
    }

    /**
     * 방 리스트 조회
     */
    @Override
    public RoomListResultResponse getRoomList(GameStatus gameStatus) {
        List<Room> rooms = roomPort.findAll();
        rooms = rooms.stream()
                .filter(room -> room.getStatus() == gameStatus)
                .toList();
        return RoomListResultResponse.fromRooms(rooms);
    }

    @Override
    public RoomDetailResponse getRoomDetail(UUID roomId) {
        Room room = roomPort.findById(roomId)
                .orElseThrow(() -> new BaseException(ErrorCode.ROOM_NOT_FOUND));
        return RoomDetailResponse.from(room);
    }
} 