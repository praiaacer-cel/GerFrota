cmake_minimum_required(VERSION 3.22.1)
project("gerfrota_llama")

# Adicione o llama.cpp como subdiretório
add_subdirectory(${CMAKE_SOURCE_DIR}/../../../../llama.cpp ${CMAKE_BINARY_DIR}/llama.cpp)

add_library(gerfrota_llama SHARED llama_bridge.cpp)

target_link_libraries(gerfrota_llama
    llama
    ggml
    log
    android
)
