# app/src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)
project(gerfrota_llama)

# Coloque o llama.cpp na RAIZ do projeto Android (irmão da pasta app/)
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../llama.cpp)
add_subdirectory(${LLAMA_DIR} llama_cpp_build)

add_library(gerfrota_llama SHARED llama_bridge.cpp)
target_include_directories(gerfrota_llama PRIVATE
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/ggml/include)
target_link_libraries(gerfrota_llama
       llama
       ggml
       android
       log
   )
