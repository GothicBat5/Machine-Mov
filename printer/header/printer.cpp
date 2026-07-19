#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"
#include <iostream>
#include <algorithm>
#include <vector>
#include <string>

struct RGB {
    int R, G, B;
};

struct CMYK {
    double C, M, Y, K;
};

CMYK ConvertToCMYK(const RGB& rgb) 
{
    double r = rgb.R / 255.0;
    double g = rgb.G / 255.0;
    double b = rgb.B / 255.0;
    double k = 1 - std::max({r, g, b});
    double denom = (1 - k == 0) ? 1 : 1 - k;
    double c = (1 - r - k) / denom;
    double m = (1 - g - k) / denom;
    double y = (1 - b - k) / denom;
    return {c, m, y, k};
}

CMYK Dither(const CMYK& cmyk) 
{
    CMYK d = cmyk;
    d.C = d.C > 0.5 ? 1 : 0;
    d.M = d.M > 0.5 ? 1 : 0;
    d.Y = d.Y > 0.5 ? 1 : 0;
    d.K = d.K > 0.5 ? 1 : 0;
    return d;
}

void FireNozzles(const CMYK& cmyk) 
{
    std::cout << "Firing nozzles: "
              << "C=" << cmyk.C << " "
              << "M=" << cmyk.M << " "
              << "Y=" << cmyk.Y << " "
              << "K=" << cmyk.K << std::endl;
}

void MovePaper() 
{
    std::cout << "Paper advanced." << std::endl;
}

std::vector<RGB> LoadImagePixels(const std::string& path, int& outWidth, int& outHeight) 
{
    int channels;
    unsigned char* data = stbi_load(path.c_str(), &outWidth, &outHeight, &channels, 3);

    if (!data) 
    {
        std::cerr << "Failed to load image: " << path << std::endl;
        std::cerr << "Reason: " << stbi_failure_reason() << std::endl;
        return {};
    }

    int total = outWidth * outHeight;
    std::vector<RGB> pixels;
    pixels.reserve(total);

    for (int i = 0; i < total; i++) 
    {
        int idx = i * 3;
        pixels.push_back({ data[idx], data[idx + 1], data[idx + 2] });
    }

    stbi_image_free(data);
    return pixels;
}

std::vector<RGB> FallbackPixels() {
    return {
        {255, 0,   0  },
        {0,   255, 0  },
        {0,   0,   255},
        {255, 255, 0  },
        {0,   0,   0  },
        {255, 255, 255}
    };
}

int main(int argc, char* argv[]) 
{
    std::vector<RGB> image;
    int width = 0, height = 0;

    if (argc > 1) 
    {
        std::string path = argv[1];
        image = LoadImagePixels(path, width, height);

        if (image.empty()) 
        {
            std::cout << "Falling back to default pixel set." << std::endl;
            image = FallbackPixels();
        } 
        else {
            std::cout << "Loaded image: " << path
                      << " (" << width << "x" << height << ", "
                      << image.size() << " pixels)" << std::endl;
        }
    } 
    else {
        std::cout << "No image path given. Using default pixel set." << std::endl;
        std::cout << "Usage: ./printer <image_path>" << std::endl;
        image = FallbackPixels();
    }

    for (const auto& pixel : image) 
    {
        CMYK cmyk = ConvertToCMYK(pixel);
        CMYK dithered = Dither(cmyk);
        FireNozzles(dithered);
        MovePaper();
    }

    std::cout << "Printing complete! (" << image.size() << " pixels processed)" << std::endl;
    return 0;
}
